package cn.strong.webapp.user.mdoptimizer.parser;

/** Parses the small envelope requested from the model and safely falls back to raw output. */
public class MarkdownOptimizationResultParser {

    private static final String START_CONTENT = "<<<OPTIMIZED_MARKDOWN>>>";
    private static final String END_CONTENT = "<<<END_OPTIMIZED_MARKDOWN>>>";
    private static final String START_SUMMARY = "<<<CHANGE_SUMMARY>>>";
    private static final String END_SUMMARY = "<<<END_CHANGE_SUMMARY>>>";

    public ParsedResult parse(String original, String raw) {
        String value = raw == null ? "" : raw.trim();
        int contentStart = value.indexOf(START_CONTENT);
        int contentEnd = value.indexOf(END_CONTENT);
        int summaryStart = value.indexOf(START_SUMMARY);
        int summaryEnd = value.indexOf(END_SUMMARY);
        if (contentStart >= 0 && contentEnd > contentStart) {
            String optimized = value.substring(contentStart + START_CONTENT.length(), contentEnd).trim();
            String summary = summaryStart >= 0 && summaryEnd > summaryStart
                    ? value.substring(summaryStart + START_SUMMARY.length(), summaryEnd).trim()
                    : "已完成 Markdown 优化。";
            if (!optimized.isEmpty()) {
                return new ParsedResult(optimized, summary);
            }
        }
        return new ParsedResult(value, "已完成 Markdown 优化。");
    }

    public static class ParsedResult {
        private final String optimizedMarkdown;
        private final String changeSummary;

        public ParsedResult(String optimizedMarkdown, String changeSummary) {
            this.optimizedMarkdown = optimizedMarkdown;
            this.changeSummary = changeSummary;
        }

        public String getOptimizedMarkdown() { return optimizedMarkdown; }
        public String getChangeSummary() { return changeSummary; }
    }
}
