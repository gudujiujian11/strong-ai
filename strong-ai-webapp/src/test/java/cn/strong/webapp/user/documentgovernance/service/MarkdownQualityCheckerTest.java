package cn.strong.webapp.user.documentgovernance.service;

import cn.strong.webapp.user.model.documentgovernance.QualityIssue;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownQualityCheckerTest {

    private final MarkdownQualityChecker checker = new MarkdownQualityChecker();

    @Test
    void findsMissingRequiredHeadingBrokenLinkAndForbiddenTermWithLineNumbers() {
        List<QualityIssue> issues = checker.check("# 概述\n访问 [站点](javascript:alert(1))\n使用 OldSDK",
                Arrays.asList("安装"), Arrays.asList(new MarkdownQualityChecker.TermRule("OldSDK", "NewSDK", true)));
        List<String> codes = issues.stream().map(QualityIssue::getRuleCode).collect(Collectors.toList());

        assertTrue(codes.contains("REQUIRED_HEADING"));
        assertTrue(codes.contains("UNSAFE_LINK"));
        assertTrue(codes.contains("FORBIDDEN_TERM"));
        assertTrue(issues.stream().allMatch(issue -> issue.getLineNumber() > 0));
    }

    @Test
    void ignoresTermsInsideFencedCodeAndDetectsUnclosedFence() {
        List<QualityIssue> issues = checker.check("```bash\n# 安装 OldSDK", Arrays.asList("安装"),
                Arrays.asList(new MarkdownQualityChecker.TermRule("OldSDK", "NewSDK", true)));
        List<String> codes = issues.stream().map(QualityIssue::getRuleCode).collect(Collectors.toList());

        assertTrue(codes.contains("UNCLOSED_FENCE"));
        assertFalse(codes.contains("FORBIDDEN_TERM"));
    }
}
