package cn.strong.webapp.user.service;

import cn.strong.webapp.user.model.request.AiChatRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CodexResponsesServiceTest {

    private final CodexResponsesService service = new CodexResponsesService(new ObjectMapper());

    @Test
    void mapsResponsesInputAndInstructionsToFixedDeepSeekModel() throws Exception {
        AiChatRequest result = service.toChatRequest(new ObjectMapper().readTree(
                "{\"model\":\"deepseek-v4.1-flash\",\"instructions\":\"你是编码助手\","
                        + "\"input\":[{\"role\":\"user\",\"content\":\"修复这个问题\"}]}"));

        assertEquals("deepseek", result.getProvider());
        assertEquals("deepseek-v4.1-flash", result.getModel());
        assertEquals(2, result.getMessages().size());
        assertEquals("system", result.getMessages().get(0).getRole());
        assertEquals("你是编码助手", result.getMessages().get(0).getContent());
        assertEquals("修复这个问题", result.getMessages().get(1).getContent());
    }

    @Test
    void rejectsUnsupportedModel() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> service.toChatRequest(
                new ObjectMapper().readTree("{\"model\":\"gpt-5\",\"input\":\"你好\"}")));
    }

    @Test
    void convertsChatCompletionChunkToResponsesDelta() throws Exception {
        String event = service.toOutputTextDelta(
                "{\"id\":\"chatcmpl-1\",\"choices\":[{\"delta\":{\"content\":\"hello\"}}]}");

        assertEquals("hello", new ObjectMapper().readTree(event).get("delta").asText());
        assertEquals("response.output_text.delta", new ObjectMapper().readTree(event).get("type").asText());
    }
}
