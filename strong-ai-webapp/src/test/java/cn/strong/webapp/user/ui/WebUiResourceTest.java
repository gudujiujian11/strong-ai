package cn.strong.webapp.user.ui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebUiResourceTest {

    @Test
    void shouldPackageChatUiResources() throws IOException {
        String html = resource("static/index.html");
        String javascript = resource("static/app.js");

        assertTrue(html.contains("id=\"sessionList\""));
        assertTrue(html.contains("id=\"modelMenu\""));
        assertTrue(html.contains("id=\"thinkingToggle\""));
        assertTrue(javascript.contains("/api/conversations"));
        assertTrue(javascript.contains("gpt-5.6-luna"));
        assertTrue(javascript.contains("deepThinking"));
        assertTrue(javascript.contains("provider: \"openai\""));
        assertTrue(javascript.contains("provider: \"deepseek\""));
        assertTrue(javascript.contains("response.body"));
        assertFalse(javascript.contains("strong-ai.conversations.v1"));
        assertFalse(javascript.toLowerCase().contains("api-key"));
    }

    @Test
    void shouldKeepTheChatViewportScrollableWhenMessagesOverflow() throws IOException {
        String styles = resource("static/styles.css");

        assertTrue(hasDeclaration(styles, ".main-panel", "min-height", "0"));
        assertTrue(hasDeclaration(styles, ".chat-viewport", "overflow-y", "auto"));
    }

    private boolean hasDeclaration(String stylesheet, String selector, String property, String value) {
        String expression = Pattern.quote(selector)
                + "\\s*\\{[^}]*" + Pattern.quote(property) + "\\s*:\\s*" + Pattern.quote(value) + "\\s*;";
        return Pattern.compile(expression, Pattern.DOTALL).matcher(stylesheet).find();
    }

    private String resource(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        assertTrue(resource.exists(), path + " should exist");
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] bytes = new byte[(int) resource.contentLength()];
            int offset = 0;
            while (offset < bytes.length) {
                int read = inputStream.read(bytes, offset, bytes.length - offset);
                if (read < 0) {
                    break;
                }
                offset += read;
            }
            return new String(bytes, 0, offset, StandardCharsets.UTF_8);
        }
    }
}
