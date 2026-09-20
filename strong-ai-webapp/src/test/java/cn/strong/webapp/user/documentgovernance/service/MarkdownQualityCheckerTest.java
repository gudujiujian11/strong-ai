package cn.strong.webapp.user.documentgovernance.service;

import cn.strong.webapp.user.documentgovernance.entity.TermEntity;
import cn.strong.webapp.user.model.documentgovernance.QualityIssue;
import cn.strong.webapp.user.model.documentgovernance.QualitySeverity;
import cn.strong.webapp.user.model.documentgovernance.RuleConfig;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownQualityCheckerTest {

    private final MarkdownQualityChecker checker = new MarkdownQualityChecker();

    @Test
    void findsMissingRequiredHeadingBrokenLinkAndForbiddenTermWithLineNumbers() {
        List<QualityIssue> issues = checker.check("# 概述\n访问 [站点](javascript:alert(1))\n使用 OldSDK",
                Arrays.asList("安装"), Arrays.asList(forbidden("OldSDK", "NewSDK")));
        List<String> codes = issues.stream().map(QualityIssue::getRuleCode).collect(Collectors.toList());

        assertTrue(codes.contains("REQUIRED_HEADING"));
        assertTrue(codes.contains("UNSAFE_LINK"));
        assertTrue(codes.contains("FORBIDDEN_TERM"));
        assertTrue(issues.stream().allMatch(issue -> issue.getLineNumber() > 0));
    }

    @Test
    void ignoresTermsInsideFencedCodeAndDetectsUnclosedFence() {
        List<QualityIssue> issues = checker.check("```bash\n# 安装 OldSDK", Arrays.asList("安装"),
                Arrays.asList(forbidden("OldSDK", "NewSDK")));
        List<String> codes = issues.stream().map(QualityIssue::getRuleCode).collect(Collectors.toList());

        assertTrue(codes.contains("UNCLOSED_FENCE"));
        assertFalse(codes.contains("FORBIDDEN_TERM"));
    }

    @Test
    void matchesForbiddenTermCaseInsensitivelyAndRespectsWordBoundary() {
        // 大小写不敏感命中
        List<String> hit = checker.check("这里用了 oldsdk", Collections.emptyList(),
                Arrays.asList(forbidden("OldSDK", "NewSDK"))).stream()
                .map(QualityIssue::getRuleCode).collect(Collectors.toList());
        assertTrue(hit.contains("FORBIDDEN_TERM"));

        // 词边界避免子串误命中：SDK 不应命中 OldSDK
        List<String> noHit = checker.check("使用 OldSDK 组件", Collections.emptyList(),
                Arrays.asList(forbidden("SDK", "开发套件"))).stream()
                .map(QualityIssue::getRuleCode).collect(Collectors.toList());
        assertFalse(noHit.contains("FORBIDDEN_TERM"));
    }

    @Test
    void suggestsPreferredTermForNonForbiddenTerm() {
        List<String> codes = checker.check("推荐使用 k8s 部署", Collections.emptyList(),
                Arrays.asList(preferred("k8s", "Kubernetes"))).stream()
                .map(QualityIssue::getRuleCode).collect(Collectors.toList());
        assertTrue(codes.contains("PREFERRED_TERM"));
    }

    @Test
    void requiredHeadingMatchesByPrefixIgnoringWhitespaceAndCase() {
        // 模板要求「安装」，文档写「安装说明」应视为满足
        List<String> codes = checker.check("# 安装说明\n内容", Arrays.asList("安装"),
                Collections.emptyList()).stream()
                .map(QualityIssue::getRuleCode).collect(Collectors.toList());
        assertFalse(codes.contains("REQUIRED_HEADING"));
    }

    @Test
    void ruleConfigCanDisableRuleAndOverrideSeverity() {
        Map<String, Boolean> enabled = new HashMap<>();
        enabled.put("FORBIDDEN_TERM", false);
        Map<String, QualitySeverity> severities = new HashMap<>();
        severities.put("UNSAFE_LINK", QualitySeverity.LOW);
        RuleConfig config = new RuleConfig(enabled, severities, null);

        List<QualityIssue> issues = checker.check("[x](javascript:alert(1)) 使用 OldSDK",
                Collections.emptyList(), Arrays.asList(forbidden("OldSDK", "NewSDK")), config);
        List<String> codes = issues.stream().map(QualityIssue::getRuleCode).collect(Collectors.toList());

        assertFalse(codes.contains("FORBIDDEN_TERM"));
        assertTrue(codes.contains("UNSAFE_LINK"));
        QualityIssue unsafe = issues.stream().filter(i -> "UNSAFE_LINK".equals(i.getRuleCode()))
                .findFirst().orElseThrow(AssertionError::new);
        assertEquals(QualitySeverity.LOW, unsafe.getSeverity());
    }

    private TermEntity forbidden(String term, String preferredTerm) {
        TermEntity entity = new TermEntity();
        entity.setTerm(term);
        entity.setPreferredTerm(preferredTerm);
        entity.setForbidden(true);
        return entity;
    }

    private TermEntity preferred(String term, String preferredTerm) {
        TermEntity entity = new TermEntity();
        entity.setTerm(term);
        entity.setPreferredTerm(preferredTerm);
        entity.setForbidden(false);
        return entity;
    }
}
