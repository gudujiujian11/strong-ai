package cn.strong.webapp.user.mdoptimizer.validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Performs cheap, deterministic checks before content is sent to a model. */
public class MarkdownDocumentValidator {

    public static final int MAX_CONTENT_LENGTH = 120000;

    public ValidationResult validate(String content, String fileName) {
        String value = content == null ? "" : content;
        if (value.trim().isEmpty()) {
            return invalid("Markdown 内容不能为空", value);
        }
        if (value.indexOf('\u0000') >= 0) {
            return invalid("Markdown 内容包含不可处理的控制字符", value);
        }
        if (value.length() > MAX_CONTENT_LENGTH) {
            return invalid("Markdown 内容不能超过 " + MAX_CONTENT_LENGTH + " 个字符", value);
        }
        if (fileName != null && !fileName.trim().isEmpty() && !isMarkdownFile(fileName)) {
            return invalid("只支持 .md 或 .markdown 文件", value);
        }
        if (fenceCount(value) % 2 != 0) {
            return invalid("Markdown 代码块未闭合", value);
        }
        return valid(value);
    }

    private boolean isMarkdownFile(String fileName) {
        String normalized = fileName.trim().toLowerCase(Locale.ROOT);
        return normalized.endsWith(".md") || normalized.endsWith(".markdown");
    }

    private int fenceCount(String content) {
        int count = 0;
        String[] lines = content.split("\\r?\\n", -1);
        for (String line : lines) {
            if (line.trim().startsWith("```")) {
                count++;
            }
        }
        return count;
    }

    private ValidationResult valid(String content) {
        return new ValidationResult(true, "校验通过", content.length(),
                content.split("\\r?\\n", -1).length, Collections.<String>emptyList());
    }

    private ValidationResult invalid(String message, String content) {
        List<String> warnings = new ArrayList<>();
        warnings.add(message);
        return new ValidationResult(false, message, content.length(),
                content.split("\\r?\\n", -1).length, warnings);
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        private final int characterCount;
        private final int lineCount;
        private final List<String> warnings;

        public ValidationResult(boolean valid, String message, int characterCount, int lineCount,
                                List<String> warnings) {
            this.valid = valid;
            this.message = message;
            this.characterCount = characterCount;
            this.lineCount = lineCount;
            this.warnings = warnings;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public int getCharacterCount() { return characterCount; }
        public int getLineCount() { return lineCount; }
        public List<String> getWarnings() { return warnings; }
    }
}
