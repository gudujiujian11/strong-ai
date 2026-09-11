package cn.strong.webapp.user.mdoptimizer;

import cn.strong.webapp.user.mdoptimizer.validator.MarkdownDocumentValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownDocumentValidatorTest {

    private final MarkdownDocumentValidator validator = new MarkdownDocumentValidator();

    @Test
    void shouldRejectEmptyContentAndUnsupportedFile() {
        assertFalse(validator.validate("  ", "notes.txt").isValid());
        assertTrue(validator.validate("# hello", "notes.md").isValid());
        assertFalse(validator.validate("# hello", "notes.txt").isValid());
    }

    @Test
    void shouldRejectUnclosedCodeFence() {
        MarkdownDocumentValidator.ValidationResult result = validator.validate("```java\nSystem.out.println(1);", "code.md");

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("代码块"));
    }

    @Test
    void shouldReportContentStatistics() {
        MarkdownDocumentValidator.ValidationResult result = validator.validate("# 标题\n\n正文", "notes.markdown");

        assertTrue(result.isValid());
        assertTrue(result.getCharacterCount() > 0);
        assertTrue(result.getLineCount() >= 3);
    }
}
