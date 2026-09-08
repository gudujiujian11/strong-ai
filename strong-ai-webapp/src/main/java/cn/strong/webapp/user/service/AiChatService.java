package cn.strong.webapp.user.service;

import cn.strong.common.exception.ParamValiateException;
import cn.strong.webapp.user.config.AiModelProperties;
import cn.strong.webapp.user.model.request.AiChatMessageRequest;
import cn.strong.webapp.user.model.request.AiChatRequest;
import cn.strong.webapp.user.model.response.AiChatResponse;
import io.github.lnyocly.ai4j.platform.dashscope.DashScopeChatService;
import io.github.lnyocly.ai4j.platform.deepseek.chat.DeepSeekChatService;
import io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatCompletion;
import io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatCompletionResponse;
import io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatMessage;
import io.github.lnyocly.ai4j.platform.openai.chat.entity.Choice;
import io.github.lnyocly.ai4j.platform.doubao.chat.DoubaoChatService;
import io.github.lnyocly.ai4j.service.IChatService;
import io.github.lnyocly.ai4j.service.Configuration;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 统一封装豆包、千问、DeepSeek 的同步聊天调用。 */
@Service
public class AiChatService {

    private final AiModelProperties properties;
    private final IChatService doubaoService;
    private final IChatService qwenService;
    private final IChatService deepseekService;

    public AiChatService(AiModelProperties properties, Configuration configuration) {
        this.properties = properties;
        this.doubaoService = new DoubaoChatService(configuration);
        this.qwenService = new DashScopeChatService(configuration);
        this.deepseekService = new DeepSeekChatService(configuration);
    }

    public AiChatResponse chat(AiChatRequest request) {
        validate(request);
        String provider = normalizeProvider(request.getProvider());
        AiModelProperties.Provider providerProperties = providerProperties(provider);
        if (isBlank(providerProperties.getApiKey())) {
            throw new ParamValiateException("未配置 " + provider + " 的 API Key");
        }

        ChatCompletion.ChatCompletionBuilder builder = ChatCompletion.builder()
                .model(request.getModel().trim())
                .messages(toMessages(request.getMessages()))
                .stream(Boolean.FALSE);
        if (request.getTemperature() != null) {
            builder.temperature(request.getTemperature());
        }
        if (request.getTopP() != null) {
            builder.topP(request.getTopP());
        }
        if (request.getMaxTokens() != null) {
            builder.maxTokens(request.getMaxTokens());
        }

        try {
            ChatCompletionResponse response = service(provider).chatCompletion(builder.build());
            return toResponse(provider, response);
        } catch (Exception ex) {
            throw new IllegalStateException("调用 " + provider + " 模型失败", ex);
        }
    }

    private IChatService service(String provider) {
        if ("doubao".equals(provider)) {
            return doubaoService;
        }
        if ("qwen".equals(provider)) {
            return qwenService;
        }
        return deepseekService;
    }

    private AiModelProperties.Provider providerProperties(String provider) {
        if ("doubao".equals(provider)) {
            return properties.getDoubao();
        }
        if ("qwen".equals(provider)) {
            return properties.getQwen();
        }
        if ("deepseek".equals(provider)) {
            return properties.getDeepseek();
        }
        throw new ParamValiateException("不支持的 provider，仅支持 doubao、qwen、deepseek");
    }

    private String normalizeProvider(String provider) {
        String normalized = provider.trim().toLowerCase(Locale.ROOT);
        return "dashscope".equals(normalized) ? "qwen" : normalized;
    }

    private void validate(AiChatRequest request) {
        if (request == null || isBlank(request.getProvider())) {
            throw new ParamValiateException("provider 不能为空");
        }
        if (isBlank(request.getModel())) {
            throw new ParamValiateException("model 不能为空");
        }
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new ParamValiateException("messages 不能为空");
        }
        if (Boolean.TRUE.equals(request.getStream())) {
            throw new ParamValiateException("当前接口暂不支持 stream=true");
        }
        for (AiChatMessageRequest message : request.getMessages()) {
            if (message == null || isBlank(message.getRole()) || isBlank(message.getContent())) {
                throw new ParamValiateException("消息 role 和 content 不能为空");
            }
        }
    }

    private List<ChatMessage> toMessages(List<AiChatMessageRequest> messages) {
        List<ChatMessage> result = new ArrayList<>(messages.size());
        for (AiChatMessageRequest message : messages) {
            result.add(new ChatMessage(message.getRole().trim(), message.getContent()));
        }
        return result;
    }

    private AiChatResponse toResponse(String provider, ChatCompletionResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new IllegalStateException("模型返回为空");
        }
        Choice choice = response.getChoices().get(0);
        ChatMessage message = choice.getMessage() == null ? choice.getDelta() : choice.getMessage();
        AiChatResponse result = new AiChatResponse();
        result.setProvider(provider);
        result.setModel(response.getModel());
        result.setId(response.getId());
        result.setContent(message == null || message.getContent() == null
                ? null : message.getContent().getText());
        result.setFinishReason(choice.getFinishReason());
        if (response.getUsage() != null) {
            result.setPromptTokens(response.getUsage().getPromptTokens());
            result.setCompletionTokens(response.getUsage().getCompletionTokens());
            result.setTotalTokens(response.getUsage().getTotalTokens());
        }
        return result;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
