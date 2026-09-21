package cn.strong.webapp.user.service;

import cn.strong.webapp.user.model.request.AiChatRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CodexResponsesServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CodexResponsesService service = new CodexResponsesService(
            objectMapper, "deepseek", CodexResponsesService.PUBLIC_MODEL);

    @Test
    void mapsResponsesInputAndInstructionsToFixedDeepSeekModel() throws Exception {
        AiChatRequest result = service.toChatRequest(objectMapper.readTree(
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
    void usesConfiguredUpstreamProviderAndModel() throws Exception {
        CodexResponsesService configuredService = new CodexResponsesService(
                objectMapper, "authorized-deepseek", "deepseek-coder");

        AiChatRequest result = configuredService.toChatRequest(objectMapper.readTree(
                "{\"model\":\"deepseek-v4.1-flash\",\"input\":\"修复这个问题\"}"));

        assertEquals("authorized-deepseek", result.getProvider());
        assertEquals("deepseek-coder", result.getModel());
    }

    @Test
    void mapsDeveloperMessagesToSystemRole() throws Exception {
        AiChatRequest result = service.toChatRequest(objectMapper.readTree(
                "{\"model\":\"deepseek-v4.1-flash\",\"input\":["
                        + "{\"role\":\"developer\",\"content\":[{\"type\":\"input_text\","
                        + "\"text\":\"遵循项目规范\"}]},"
                        + "{\"role\":\"user\",\"content\":\"修复这个问题\"}]}"));

        assertEquals("system", result.getMessages().get(0).getRole());
        assertEquals("遵循项目规范", result.getMessages().get(0).getContent());
    }

    @Test
    void rejectsRolesUnsupportedByChatCompletions() throws Exception {
        JsonNode request = objectMapper.readTree(
                "{\"model\":\"deepseek-v4.1-flash\",\"input\":["
                        + "{\"role\":\"channel\",\"content\":\"invalid\"}]}" );

        assertThrows(IllegalArgumentException.class, () -> service.toChatRequest(request));
    }

    @Test
    void rejectsUnsupportedModel() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> service.toChatRequest(
                objectMapper.readTree("{\"model\":\"gpt-5\",\"input\":\"你好\"}")));
    }

    @Test
    void convertsChatCompletionChunkToResponsesDelta() throws Exception {
        String event = service.toOutputTextDelta(
                "{\"id\":\"chatcmpl-1\",\"choices\":[{\"delta\":{\"content\":\"hello\"}}]}");

        assertEquals("hello", objectMapper.readTree(event).get("delta").asText());
        assertEquals("response.output_text.delta", objectMapper.readTree(event).get("type").asText());
    }
}
