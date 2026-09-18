package cn.strong.webapp.user.documentgovernance.service;

import cn.strong.webapp.user.documentgovernance.entity.TermEntity;
import cn.strong.webapp.user.model.documentgovernance.QualityIssue;
import cn.strong.webapp.user.model.documentgovernance.QualitySeverity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class MarkdownQualityChecker {

    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*$");
    private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[[^]]*]\\(([^)]+)\\)");

    public List<QualityIssue> check(String content, List<String> requiredHeadings, List<TermEntity> terms) {
        if (content == null || content.isEmpty()) {
            return Collections.singletonList(issue("EMPTY_DOCUMENT", QualitySeverity.HIGH, 1,
                    "文档内容为空", "补充 Markdown 文档内容"));
        }
        List<QualityIssue> issues = new ArrayList<>();
        List<String> headings = new ArrayList<>();
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
                if (previousHeadingLevel > 0 && level > previousHeadingLevel + 1) {
                    issues.add(issue("HEADING_ORDER", QualitySeverity.LOW, lineNumber,
                            "标题层级从 H" + previousHeadingLevel + " 跳至 H" + level,
                            "逐级使用 Markdown 标题层级"));
                }
                previousHeadingLevel = level;
            }
            Matcher link = MARKDOWN_LINK.matcher(line);
            while (link.find()) {
                String target = link.group(1).trim().toLowerCase(java.util.Locale.ROOT);
                if (target.startsWith("javascript:") || target.startsWith("data:")) {
                    issues.add(issue("UNSAFE_LINK", QualitySeverity.HIGH, lineNumber,
                            "文档包含危险链接协议", "改用 HTTPS 链接或相对资源路径"));
                }
            }
            for (TermEntity term : safeTerms(terms)) {
                if (Boolean.TRUE.equals(term.getForbidden()) && !term.getTerm().isEmpty() && line.contains(term.getTerm())) {
                    issues.add(issue("FORBIDDEN_TERM", QualitySeverity.MEDIUM, lineNumber,
                            "检测到禁用术语：" + term.getTerm(), "建议使用：" + term.getPreferredTerm()));
                }
            }
        }
        if (fenced) {
            issues.add(issue("UNCLOSED_FENCE", QualitySeverity.HIGH, lines.length,
                    "代码围栏未闭合", "补充对应的 ``` 结束代码块"));
        }
        for (String required : safeHeadings(requiredHeadings)) {
            if (!headings.contains(required)) {
                issues.add(issue("REQUIRED_HEADING", QualitySeverity.MEDIUM, 1,
                        "缺少必填章节：" + required, "添加 “# " + required + "” 章节"));
            }
        }
        return issues;
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
}
