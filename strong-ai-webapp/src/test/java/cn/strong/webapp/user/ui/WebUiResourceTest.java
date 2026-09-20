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
    void shouldPackageMarkdownOptimizerUiResources() throws IOException {
        String html = resource("static/md-optimizer.html");
        String javascript = resource("static/md-optimizer.js");
        String styles = resource("static/md-optimizer.css");

        assertTrue(html.contains("id=\"markdownInput\""));
        assertTrue(html.contains("id=\"optimizeButton\""));
        assertTrue(html.contains("id=\"fileInput\""));
        assertTrue(html.contains("data-source-tab=\"image\""));
        assertTrue(html.contains("id=\"imageInput\""));
        assertTrue(html.contains("id=\"parseImageButton\""));
        assertTrue(html.contains("id=\"modelSelect\""));
        assertTrue(javascript.contains("/api/md-optimizer/validate"));
        assertTrue(javascript.contains("/api/md-optimizer/optimize"));
        assertTrue(styles.contains(".optimizer-shell"));
        assertTrue(hasDeclaration(styles, ".result-empty[hidden], .result-summary[hidden]", "display", "none"));
        assertTrue(html.contains(".zip"));
        assertTrue(javascript.contains("JSZip"));
        assertTrue(javascript.contains("MAX_ARCHIVE_SIZE"));
        assertTrue(javascript.contains("恰好包含一个 Markdown 文件"));
        assertTrue(javascript.contains("generateAsync"));
        assertTrue(javascript.contains("state.archive ? state.archive.markdownPath"));
        assertTrue(styles.contains(".asset-link"));
        assertTrue(javascript.contains("/api/md-optimizer/parse-image"));
        assertTrue(javascript.contains("FormData"));
    }

    @Test
    void shouldKeepTheChatViewportScrollableWhenMessagesOverflow() throws IOException {
        String styles = resource("static/styles.css");

        assertTrue(hasDeclaration(styles, ".main-panel", "min-height", "0"));
        assertTrue(hasDeclaration(styles, ".chat-viewport", "overflow-y", "auto"));
    }

    @Test
    void documentGovernanceResourcesExposeTheRequiredWorkflow() throws IOException {
        String html = resource("static/document-governance.html");
        String javascript = resource("static/document-governance.js");
        String styles = resource("static/document-governance.css");

        assertTrue(html.contains("document-governance.js"));
        assertTrue(html.contains("文档列表"));
        assertTrue(html.contains("质量检查"));
        assertTrue(html.contains("版本历史"));
        assertTrue(html.contains("术语库"));
        assertTrue(html.contains("id=\"aiModelSelect\""));
        assertTrue(javascript.contains("/api/document-governance/documents"));
        assertTrue(javascript.contains("ai-tasks"));
        assertTrue(html.contains("gpt-5.6-luna"));
        assertTrue(javascript.contains("els.aiModelSelect.value"));
        assertTrue(javascript.contains("quality-check"));
        assertTrue(javascript.contains("versions"));
        assertFalse(javascript.contains("localStorage.setItem(\"document"));
        assertTrue(styles.contains(".governance-workspace"));
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
