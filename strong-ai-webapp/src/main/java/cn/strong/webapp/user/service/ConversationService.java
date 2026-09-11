package cn.strong.webapp.user.service;

import cn.strong.common.exception.ParamValiateException;
import cn.strong.webapp.user.conversation.ConversationStore;
import cn.strong.webapp.user.model.conversation.Conversation;
import cn.strong.webapp.user.model.conversation.ConversationMessage;
import cn.strong.webapp.user.model.request.AiChatMessageRequest;
import cn.strong.webapp.user.model.request.AiChatRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ConversationService {

    private static final String DEFAULT_TITLE = "新对话";
    private final ConversationStore conversationStore;

    public ConversationService(ConversationStore conversationStore) {
        this.conversationStore = conversationStore;
    }

    public synchronized List<Conversation> list() {
        return conversationStore.findAll();
    }

    public synchronized Conversation get(String id) {
        return load(id);
    }

    public synchronized Conversation create(String provider, String model) {
        requireProviderAndModel(provider, model);
        long now = System.currentTimeMillis();
        Conversation conversation = new Conversation();
        conversation.setId(UUID.randomUUID().toString());
        conversation.setTitle(DEFAULT_TITLE);
        conversation.setProvider(provider.trim());
        conversation.setModel(model.trim());
        conversation.setDeepThinking(Boolean.FALSE);
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        return conversationStore.save(conversation);
    }

    public synchronized Conversation updateModel(String id, String provider, String model) {
        return updateModel(id, provider, model, null);
    }

    public synchronized Conversation updateModel(String id, String provider, String model, Boolean deepThinking) {
        requireProviderAndModel(provider, model);
        Conversation conversation = load(id);
        conversation.setProvider(provider.trim());
        conversation.setModel(model.trim());
        if (deepThinking != null) {
            conversation.setDeepThinking(deepThinking);
        }
        conversation.setUpdatedAt(System.currentTimeMillis());
        return conversationStore.save(conversation);
    }

    public synchronized Conversation clearMessages(String id) {
        Conversation conversation = load(id);
        conversation.setMessages(new ArrayList<>());
        conversation.setTitle(DEFAULT_TITLE);
        conversation.setUpdatedAt(System.currentTimeMillis());
        return conversationStore.save(conversation);
    }

    public synchronized boolean delete(String id) {
        return conversationStore.deleteById(id);
    }

    public synchronized AiChatRequest prepareChatRequest(String id, String content) {
        if (isBlank(content)) {
            throw new ParamValiateException("消息内容不能为空");
        }
        Conversation conversation = load(id);
        ConversationMessage message = message("user", content.trim(), null, null, null);
        conversation.getMessages().add(message);
        if (conversation.getMessages().size() == 1) {
            conversation.setTitle(makeTitle(message.getContent()));
        }
        conversation.setUpdatedAt(System.currentTimeMillis());
        conversationStore.save(conversation);
        return toChatRequest(conversation);
    }

    public synchronized AiChatRequest prepareRegenerateRequest(String id, String assistantMessageId) {
        Conversation conversation = load(id);
        boolean found = false;
        Iterator<ConversationMessage> iterator = conversation.getMessages().iterator();
        while (iterator.hasNext()) {
            ConversationMessage message = iterator.next();
            if (assistantMessageId.equals(message.getId()) && "assistant".equals(message.getRole())) {
                found = true;
                iterator.remove();
                break;
            }
        }
        if (!found) {
            throw new ParamValiateException("未找到可重新生成的助手消息");
        }
        conversation.setUpdatedAt(System.currentTimeMillis());
        conversationStore.save(conversation);
        return toChatRequest(conversation);
    }

    public synchronized Conversation appendAssistantReply(String id, String content, String reasoning) {
        return appendAssistantReply(id, content, reasoning, null, null, null);
    }

    public synchronized Conversation appendAssistantReply(String id, String content, String reasoning,
                                                           Long promptTokens, Long completionTokens,
                                                           Long totalTokens) {
        Conversation conversation = load(id);
        if (isBlank(content) && isBlank(reasoning)) {
            return conversation;
        }
        ConversationMessage message = message("assistant", content, reasoning,
                conversation.getProvider(), conversation.getModel());
        message.setPromptTokens(promptTokens);
        message.setCompletionTokens(completionTokens);
        message.setTotalTokens(totalTokens);
        conversation.getMessages().add(message);
        conversation.setUpdatedAt(System.currentTimeMillis());
        return conversationStore.save(conversation);
    }

    private AiChatRequest toChatRequest(Conversation conversation) {
        AiChatRequest request = new AiChatRequest();
        request.setProvider(conversation.getProvider());
        request.setModel(conversation.getModel());
        request.setDeepThinking(conversation.getDeepThinking());
        List<AiChatMessageRequest> messages = new ArrayList<>();
        for (ConversationMessage message : conversation.getMessages()) {
            if (!isBlank(message.getContent())) {
                messages.add(new AiChatMessageRequest(message.getRole(), message.getContent()));
            }
        }
        request.setMessages(messages);
        request.setStream(Boolean.TRUE);
        return request;
    }

    private ConversationMessage message(String role, String content, String reasoning, String provider, String model) {
        ConversationMessage message = new ConversationMessage();
        message.setId(UUID.randomUUID().toString());
        message.setRole(role);
        message.setContent(content == null ? "" : content);
        message.setReasoning(reasoning == null ? "" : reasoning);
        message.setProvider(provider);
        message.setModel(model);
        message.setCreatedAt(System.currentTimeMillis());
        return message;
    }

    private Conversation load(String id) {
        if (isBlank(id)) {
            throw new ParamValiateException("会话 ID 不能为空");
        }
        return conversationStore.findById(id)
                .orElseThrow(() -> new ParamValiateException("会话不存在或已过期"));
    }

    private void requireProviderAndModel(String provider, String model) {
        if (isBlank(provider) || isBlank(model)) {
            throw new ParamValiateException("provider 和 model 不能为空");
        }
    }

    private String makeTitle(String content) {
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() > 26 ? normalized.substring(0, 26) + "…" : normalized;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
