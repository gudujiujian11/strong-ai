package cn.strong.webapp.user.service;

import cn.strong.webapp.user.model.request.AiChatMessageRequest;
import cn.strong.webapp.user.model.request.AiChatRequest;
import cn.strong.webapp.user.model.response.AiChatResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** Codex Responses 协议与现有 Chat Completions 服务之间的适配器。 */
@Service
public class CodexResponsesService {

    public static final String PUBLIC_MODEL = "deepseek-v4.1-flash";
    private static final String PROVIDER = "deepseek";
    private final ObjectMapper objectMapper;

    public CodexResponsesService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AiChatRequest toChatRequest(JsonNode request) {
        String model = text(request, "model");
        if (!PUBLIC_MODEL.equals(model)) {
            throw new IllegalArgumentException("model 仅支持 " + PUBLIC_MODEL);
        }

        List<AiChatMessageRequest> messages = new ArrayList<>();
        String instructions = text(request, "instructions");
        if (!isBlank(instructions)) {
            messages.add(new AiChatMessageRequest("system", instructions));
        }
        JsonNode input = request.get("input");
        if (input == null || input.isNull()) {
            throw new IllegalArgumentException("input 不能为空");
        }
        if (input.isTextual()) {
            messages.add(new AiChatMessageRequest("user", input.asText()));
        } else if (input.isArray()) {
            for (JsonNode item : input) {
                String role = text(item, "role");
                String content = content(item.get("content"));
                if (isBlank(role) || isBlank(content)) {
                    throw new IllegalArgumentException("input 消息的 role 和 content 不能为空");
                }
                messages.add(new AiChatMessageRequest(role, content));
            }
        } else {
            throw new IllegalArgumentException("input 必须是字符串或消息数组");
        }
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("input 不能为空");
        }

        AiChatRequest result = new AiChatRequest();
        result.setProvider(PROVIDER);
        result.setModel(PUBLIC_MODEL);
        result.setMessages(messages);
        result.setDeepThinking("high".equals(request.path("reasoning").path("effort").asText()));
        result.setTemperature(number(request, "temperature", Float.class));
        result.setTopP(number(request, "top_p", Float.class));
        result.setMaxTokens(number(request, "max_output_tokens", Integer.class));
        result.setStream(Boolean.FALSE);
        return result;
    }

    public String toResponse(AiChatResponse response) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("id", response.getId() == null ? "response-unknown" : response.getId());
        result.put("object", "response");
        result.put("model", PUBLIC_MODEL);
        result.put("status", "completed");
        ArrayNode output = result.putArray("output");
        ObjectNode message = output.addObject();
        message.put("type", "message");
        message.put("role", "assistant");
        ArrayNode content = message.putArray("content");
        ObjectNode text = content.addObject();
        text.put("type", "output_text");
        text.put("text", response.getContent() == null ? "" : response.getContent());
        return write(result);
    }

    public String toOutputTextDelta(String chunk) {
        try {
            JsonNode root = objectMapper.readTree(chunk);
            JsonNode delta = root.path("choices").path(0).path("delta").path("content");
            if (delta.isMissingNode() || delta.isNull() || delta.asText().isEmpty()) {
                return null;
            }
            ObjectNode event = objectMapper.createObjectNode();
            event.put("type", "response.output_text.delta");
            event.put("delta", delta.asText());
            event.put("item_id", root.path("id").asText("response-output"));
            return write(event);
        } catch (Exception ex) {
            throw new IllegalArgumentException("上游流式数据格式无效", ex);
        }
    }

    public String completedEvent() {
        ObjectNode event = objectMapper.createObjectNode();
        event.put("type", "response.completed");
        event.put("model", PUBLIC_MODEL);
        return write(event);
    }

    private String content(JsonNode content) {
        if (content == null || content.isNull()) {
            return null;
        }
        if (content.isTextual()) {
            return content.asText();
        }
        if (content.isArray()) {
            StringBuilder result = new StringBuilder();
            for (JsonNode part : content) {
                String text = text(part, "text");
                if (!isBlank(text)) {
                    if (result.length() > 0) {
                        result.append('\n');
                    }
                    result.append(text);
                }
            }
            return result.toString();
        }
        return null;
    }

    private <T extends Number> T number(JsonNode request, String name, Class<T> type) {
        JsonNode node = request.get(name);
        if (node == null || !node.isNumber()) {
            return null;
        }
        if (type == Float.class) {
            return type.cast(node.floatValue());
        }
        return type.cast(node.intValue());
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String write(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            throw new IllegalStateException("生成 Responses 响应失败", ex);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
