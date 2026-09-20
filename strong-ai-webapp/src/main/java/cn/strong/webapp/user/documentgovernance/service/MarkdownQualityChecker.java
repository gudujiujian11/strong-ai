package cn.strong.webapp.user.documentgovernance.service;

import cn.strong.webapp.user.documentgovernance.entity.TermEntity;
import cn.strong.webapp.user.model.documentgovernance.QualityIssue;
import cn.strong.webapp.user.model.documentgovernance.QualitySeverity;
import cn.strong.webapp.user.model.documentgovernance.RuleConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class MarkdownQualityChecker {

    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*$");
    private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[[^]]*]\\(([^)]+)\\)");
    private static final Pattern ASCII_ONLY = Pattern.compile("^[\\x00-\\x7F]+$");

    /** 兼容旧调用：使用默认规则配置。 */
    public List<QualityIssue> check(String content, List<String> requiredHeadings, List<TermEntity> terms) {
        return check(content, requiredHeadings, terms, RuleConfig.defaults());
    }

    public List<QualityIssue> check(String content, List<String> requiredHeadings, List<TermEntity> terms,
                                    RuleConfig config) {
        RuleConfig ruleConfig = config == null ? RuleConfig.defaults() : config;
        if (content == null || content.isEmpty()) {
            if (!ruleConfig.isEnabled("EMPTY_DOCUMENT")) {
                return Collections.emptyList();
            }
            return Collections.singletonList(issue("EMPTY_DOCUMENT",
                    ruleConfig.severity("EMPTY_DOCUMENT", QualitySeverity.HIGH), 1,
                    "文档内容为空", "补充 Markdown 文档内容"));
        }
        List<QualityIssue> issues = new ArrayList<>();
        List<String> headings = new ArrayList<>();
        List<TermMatcher> termMatchers = compileTerms(terms);
        String[] lines = content.split("\\r?\\n", -1);
        boolean fenced = false;
        int previousHeadingLevel = 0;
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            int lineNumber = index + 1;
            if (line.trim().startsWith("```")) {
                fenced = !fenced;
                continue;
            }
            if (fenced) {
                continue;
            }
            Matcher heading = HEADING.matcher(line);
            if (heading.matches()) {
                int level = heading.group(1).length();
                headings.add(heading.group(2).trim());
                if (ruleConfig.isEnabled("HEADING_ORDER")
                        && previousHeadingLevel > 0 && level > previousHeadingLevel + 1) {
                    issues.add(issue("HEADING_ORDER", ruleConfig.severity("HEADING_ORDER", QualitySeverity.LOW),
                            lineNumber, "标题层级从 H" + previousHeadingLevel + " 跳至 H" + level,
                            "逐级使用 Markdown 标题层级"));
                }
                previousHeadingLevel = level;
            }
            if (ruleConfig.isEnabled("UNSAFE_LINK")) {
                Matcher link = MARKDOWN_LINK.matcher(line);
                while (link.find()) {
                    String target = link.group(1).trim().toLowerCase(Locale.ROOT);
                    if (target.startsWith("javascript:") || target.startsWith("data:")) {
                        issues.add(issue("UNSAFE_LINK", ruleConfig.severity("UNSAFE_LINK", QualitySeverity.HIGH),
                                lineNumber, "文档包含危险链接协议", "改用 HTTPS 链接或相对资源路径"));
                    }
                }
            }
            for (TermMatcher matcher : termMatchers) {
                if (!matcher.pattern.matcher(line).find()) {
                    continue;
                }
                TermEntity term = matcher.term;
                if (Boolean.TRUE.equals(term.getForbidden())) {
                    if (ruleConfig.isEnabled("FORBIDDEN_TERM")) {
                        issues.add(issue("FORBIDDEN_TERM",
                                ruleConfig.severity("FORBIDDEN_TERM", QualitySeverity.MEDIUM), lineNumber,
                                "检测到禁用术语：" + term.getTerm(), "建议使用：" + safe(term.getPreferredTerm())));
                    }
                } else if (ruleConfig.isEnabled("PREFERRED_TERM") && hasPreferredReplacement(term)) {
                    issues.add(issue("PREFERRED_TERM",
                            ruleConfig.severity("PREFERRED_TERM", QualitySeverity.LOW), lineNumber,
                            "术语 “" + term.getTerm() + "” 建议使用推荐写法",
                            "建议替换为：" + term.getPreferredTerm()));
                }
            }
        }
        if (fenced && ruleConfig.isEnabled("UNCLOSED_FENCE")) {
            issues.add(issue("UNCLOSED_FENCE", ruleConfig.severity("UNCLOSED_FENCE", QualitySeverity.HIGH),
                    lines.length, "代码围栏未闭合", "补充对应的 ``` 结束代码块"));
        }
        if (ruleConfig.isEnabled("REQUIRED_HEADING")) {
            for (String required : safeHeadings(requiredHeadings)) {
                if (!headingSatisfied(headings, required)) {
                    issues.add(issue("REQUIRED_HEADING",
                            ruleConfig.severity("REQUIRED_HEADING", QualitySeverity.MEDIUM), 1,
                            "缺少必填章节：" + required, "添加 “# " + required + "” 章节"));
                }
            }
        }
        return issues;
    }

    private List<TermMatcher> compileTerms(List<TermEntity> terms) {
        List<TermMatcher> matchers = new ArrayList<>();
        for (TermEntity term : safeTerms(terms)) {
            String raw = term.getTerm();
            if (raw == null || raw.trim().isEmpty()) {
                continue;
            }
            String word = raw.trim();
            String regex = Pattern.quote(word);
            // 纯 ASCII 术语加词边界，避免 SDK 命中 OldSDK；CJK 无可靠词边界，保持子串匹配。
            if (ASCII_ONLY.matcher(word).matches()) {
                regex = "\\b" + regex + "\\b";
            }
            matchers.add(new TermMatcher(term, Pattern.compile(regex, Pattern.CASE_INSENSITIVE)));
        }
        return matchers;
    }

    /** 归一化后命中：忽略大小写与空白，且允许文档标题以必填章节名开头。 */
    private boolean headingSatisfied(List<String> headings, String required) {
        String target = normalize(required);
        if (target.isEmpty()) {
            return true;
        }
        for (String heading : headings) {
            String normalized = normalize(heading);
            if (normalized.equals(target) || normalized.startsWith(target)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private boolean hasPreferredReplacement(TermEntity term) {
        String preferred = term.getPreferredTerm();
        return preferred != null && !preferred.trim().isEmpty()
                && !preferred.trim().equalsIgnoreCase(term.getTerm() == null ? "" : term.getTerm().trim());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private List<String> safeHeadings(List<String> headings) {
        return headings == null ? Collections.emptyList() : headings;
    }

    private List<TermEntity> safeTerms(List<TermEntity> terms) {
        return terms == null ? Collections.emptyList() : terms;
    }

    private QualityIssue issue(String code, QualitySeverity severity, int lineNumber,
                               String message, String suggestion) {
        QualityIssue issue = new QualityIssue();
        issue.setRuleCode(code);
        issue.setSeverity(severity);
        issue.setLineNumber(lineNumber);
        issue.setMessage(message);
        issue.setSuggestion(suggestion);
        return issue;
    }

    private static final class TermMatcher {
        private final TermEntity term;
        private final Pattern pattern;

        private TermMatcher(TermEntity term, Pattern pattern) {
            this.term = term;
            this.pattern = pattern;
        }
    }
}
