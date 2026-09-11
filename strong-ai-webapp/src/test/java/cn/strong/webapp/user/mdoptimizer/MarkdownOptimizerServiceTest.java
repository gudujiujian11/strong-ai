package cn.strong.webapp.user.mdoptimizer;

import cn.strong.webapp.user.mdoptimizer.model.MarkdownOptimizeRequest;
import cn.strong.webapp.user.mdoptimizer.service.MarkdownOptimizerService;
import cn.strong.webapp.user.service.AiChatService;
import cn.strong.webapp.user.model.response.AiChatResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.mockito.ArgumentCaptor;
import cn.strong.webapp.user.model.request.AiChatRequest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MarkdownOptimizerServiceTest {

    @Test
    void shouldBuildMarkdownSpecificPromptAndParseModelResult() {
        AiChatService aiChatService = mock(AiChatService.class);
        AiChatResponse aiResponse = new AiChatResponse();
        aiResponse.setProvider("openai");
        aiResponse.setModel("gpt-5.6-sol");
        aiResponse.setContent("<<<OPTIMIZED_MARKDOWN>>>\n# 新标题\n<<<END_OPTIMIZED_MARKDOWN>>>\n"
                + "<<<CHANGE_SUMMARY>>>\n- 修复标题\n<<<END_CHANGE_SUMMARY>>>");
        when(aiChatService.chat(any())).thenReturn(aiResponse);

        MarkdownOptimizeRequest request = new MarkdownOptimizeRequest();
        request.setContent("# 旧标题");
        request.setFileName("README.md");
        request.setMode("format");
        request.setDocumentType("README");

        MarkdownOptimizerService service = new MarkdownOptimizerService(aiChatService);
        cn.strong.webapp.user.mdoptimizer.model.MarkdownOptimizeResponse result = service.optimize(request);

        assertEquals("# 新标题", result.getOptimizedContent());
        assertEquals("- 修复标题", result.getChangeSummary());
        ArgumentCaptor<AiChatRequest> captor = ArgumentCaptor.forClass(AiChatRequest.class);
        org.mockito.Mockito.verify(aiChatService).chat(captor.capture());
        assertTrue(captor.getValue().getMessages().get(0).getContent().contains("仅格式优化"));
        assertTrue(captor.getValue().getMessages().get(0).getContent().contains("README"));
    }

    @Test
    void shouldApplyProtectionSwitchesToPrompt() {
        AiChatService aiChatService = mock(AiChatService.class);
        MarkdownOptimizeRequest request = new MarkdownOptimizeRequest();
        request.setContent("# 标题");
        request.setPreserveCode(Boolean.FALSE);
        request.setPreserveLinks(Boolean.FALSE);

        String systemPrompt = new MarkdownOptimizerService(aiChatService)
                .buildChatRequest(request).getMessages().get(0).getContent();

        assertTrue(systemPrompt.contains("代码块可在必要时调整"));
        assertTrue(systemPrompt.contains("链接可在必要时调整"));
    }
}
