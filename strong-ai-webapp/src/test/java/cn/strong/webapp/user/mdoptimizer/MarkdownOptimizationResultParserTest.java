package cn.strong.webapp.user.mdoptimizer;

import cn.strong.webapp.user.mdoptimizer.parser.MarkdownOptimizationResultParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkdownOptimizationResultParserTest {

    private final MarkdownOptimizationResultParser parser = new MarkdownOptimizationResultParser();

    @Test
    void shouldExtractOptimizedMarkdownAndSummary() {
        String raw = "<<<OPTIMIZED_MARKDOWN>>>\n# 新标题\n<<<END_OPTIMIZED_MARKDOWN>>>\n"
                + "<<<CHANGE_SUMMARY>>>\n- 修复标题层级\n<<<END_CHANGE_SUMMARY>>>";

        MarkdownOptimizationResultParser.ParsedResult result = parser.parse("# 旧标题", raw);

        assertEquals("# 新标题", result.getOptimizedMarkdown());
        assertEquals("- 修复标题层级", result.getChangeSummary());
    }

    @Test
    void shouldFallbackToRawResponseWhenMarkersAreMissing() {
        MarkdownOptimizationResultParser.ParsedResult result = parser.parse("# 原文", "# 优化结果");

        assertEquals("# 优化结果", result.getOptimizedMarkdown());
        assertEquals("已完成 Markdown 优化。", result.getChangeSummary());
    }
}
