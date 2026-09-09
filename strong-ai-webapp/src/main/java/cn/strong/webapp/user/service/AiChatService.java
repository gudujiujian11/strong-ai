package cn.strong.webapp.user.service;

import cn.strong.common.exception.ParamValiateException;
import cn.strong.webapp.user.model.request.AiChatMessageRequest;
import cn.strong.webapp.user.model.request.AiChatRequest;
import cn.strong.webapp.user.model.response.AiChatResponse;
import io.github.lnyocly.ai4j.listener.SseListener;
import io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatCompletion;
import io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatCompletionResponse;
import io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatMessage;
import io.github.lnyocly.ai4j.platform.openai.chat.entity.Choice;
import io.github.lnyocly.ai4j.service.IChatService;
import io.github.lnyocly.ai4j.service.factory.AiServiceRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 通过 ai4j starter 的多模型注册表统一调用聊天模型。 */
@Service
public class AiChatService {

    private final AiServiceRegistry aiServiceRegistry;

    public AiChatService(AiServiceRegistry aiServiceRegistry) {
        this.aiServiceRegistry = aiServiceRegistry;
    }

    public AiChatResponse chat(AiChatRequest request) {
        validate(request, false);
        String provider = normalizeProvider(request.getProvider());
        IChatService chatService = service(provider);

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
            setMaxTokens(builder, request.getMaxTokens(), provider, request.getModel());
        }
        setDeepThinking(builder, request, provider);

        try {
            ChatCompletionResponse response = chatService.chatCompletion(builder.build());
            return toResponse(provider, response);
        } catch (Exception ex) {
            throw new IllegalStateException("调用 " + provider + " 模型失败", ex);
        }
    }

    public void chatStream(AiChatRequest request, SseListener listener) {
        validate(request, true);
        String provider = normalizeProvider(request.getProvider());
        IChatService chatService = service(provider);

        ChatCompletion.ChatCompletionBuilder builder = ChatCompletion.builder()
                .model(request.getModel().trim())
                .messages(toMessages(request.getMessages()))
                .stream(Boolean.TRUE);
        if (request.getTemperature() != null) {
            builder.temperature(request.getTemperature());
        }
        if (request.getTopP() != null) {
            builder.topP(request.getTopP());
        }
        if (request.getMaxTokens() != null) {
            setMaxTokens(builder, request.getMaxTokens(), provider, request.getModel());
        }
        setDeepThinking(builder, request, provider);

        try {
            chatService.chatCompletionStream(builder.build(), listener);
            if (listener.getFailure() != null) {
                throw new IllegalStateException(listener.getFailure());
            }
        } catch (Exception ex) {
            throw new IllegalStateException("调用 " + provider + " 流式模型失败", ex);
        }
    }

    private IChatService service(String provider) {
        if (!aiServiceRegistry.contains(provider)) {
            throw new ParamValiateException("未配置或不支持的 provider: " + provider);
        }
        return aiServiceRegistry.getChatService(provider);
    }

    private String normalizeProvider(String provider) {
        String normalized = provider.trim().toLowerCase(Locale.ROOT);
        return "dashscope".equals(normalized) ? "qwen" : normalized;
    }

    private void setMaxTokens(ChatCompletion.ChatCompletionBuilder builder, Integer maxTokens,
                              String provider, String model) {
        if ("openai".equals(provider)
                && model.trim().toLowerCase(Locale.ROOT).startsWith("gpt-5.6")) {
            builder.maxCompletionTokens(maxTokens);
            return;
        }
        builder.maxTokens(maxTokens);
    }

    private void setDeepThinking(ChatCompletion.ChatCompletionBuilder builder, AiChatRequest request,
                                 String provider) {
        if ("openai".equals(provider)
                && request.getModel().trim().toLowerCase(Locale.ROOT).startsWith("gpt-5.6")) {
            builder.parameter("reasoning_effort", Boolean.TRUE.equals(request.getDeepThinking()) ? "medium" : "none");
        }
    }

    private void validate(AiChatRequest request, boolean stream) {
        if (request == null || isBlank(request.getProvider())) {
            throw new ParamValiateException("provider 不能为空");
        }
        if (isBlank(request.getModel())) {
            throw new ParamValiateException("model 不能为空");
        }
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new ParamValiateException("messages 不能为空");
        }
        if (!stream && Boolean.TRUE.equals(request.getStream())) {
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
