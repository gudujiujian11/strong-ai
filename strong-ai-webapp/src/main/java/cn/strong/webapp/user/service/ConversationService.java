package cn.strong.webapp.user.service;

import cn.strong.common.exception.ParamValiateException;
import cn.strong.webapp.user.conversation.ConversationStore;
import cn.strong.webapp.user.model.conversation.Conversation;
import cn.strong.webapp.user.model.conversation.ConversationAttachment;
import cn.strong.webapp.user.model.conversation.ConversationMessage;
import cn.strong.webapp.user.model.request.AiChatContentPart;
import cn.strong.webapp.user.model.request.AiChatMessageRequest;
import cn.strong.webapp.user.model.request.AiChatRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationService {

    private static final String DEFAULT_TITLE = "新对话";
    private static final String IMAGE_PROMPT = "请描述并分析这张图片";
    private final ConversationStore conversationStore;
    private final ConversationAttachmentService attachmentService;

    public ConversationService(ConversationStore conversationStore) {
        this(conversationStore, null);
    }

    @Autowired
    public ConversationService(ConversationStore conversationStore,
                               ConversationAttachmentService attachmentService) {
        this.conversationStore = conversationStore;
        this.attachmentService = attachmentService;
    }

    public synchronized List<Conversation> list() {
        List<Conversation> conversations = conversationStore.findAll();
        if (attachmentService != null) {
            attachmentService.enrich(conversations);
        }
        return conversations;
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
        if (containsImages(conversation) && !supportsImageInput(provider, model)) {
            throw new ParamValiateException("当前会话包含图片，所选模型不支持图片输入");
        }
        conversation.setProvider(provider.trim());
        conversation.setModel(model.trim());
        if (deepThinking != null) {
            conversation.setDeepThinking(deepThinking);
        }
        conversation.setUpdatedAt(System.currentTimeMillis());
        return conversationStore.save(conversation);
    }

    @Transactional(rollbackFor = Exception.class)
    public synchronized Conversation clearMessages(String id) {
        Conversation conversation = load(id);
        if (attachmentService != null) {
            attachmentService.deleteByConversationId(id);
        }
        conversation.setMessages(new ArrayList<>());
        conversation.setTitle(DEFAULT_TITLE);
        conversation.setUpdatedAt(System.currentTimeMillis());
        return conversationStore.save(conversation);
    }

    @Transactional(rollbackFor = Exception.class)
    public synchronized boolean delete(String id) {
        if (attachmentService != null) {
            attachmentService.deleteByConversationId(id);
        }
        return conversationStore.deleteById(id);
    }

    public synchronized AiChatRequest prepareChatRequest(String id, String content) {
        return prepareChat(id, content, Collections.<String>emptyList()).getRequest();
    }

    @Transactional(rollbackFor = Exception.class)
    public synchronized PreparedChat prepareChat(String id, String content, List<String> attachmentIds) {
        List<String> ids = attachmentIds == null ? Collections.<String>emptyList() : attachmentIds;
        if (isBlank(content) && ids.isEmpty()) {
            throw new ParamValiateException("消息内容或图片不能为空");
        }
        Conversation conversation = load(id);
        List<ConversationAttachment> attachments = attachmentService == null
                ? Collections.<ConversationAttachment>emptyList()
                : attachmentService.validateUnbound(id, ids);
        if (!attachments.isEmpty() && !supportsImageInput(conversation.getProvider(), conversation.getModel())) {
            throw new ParamValiateException("当前模型不支持图片输入，请切换到支持图片的模型");
        }
        ConversationMessage message = message("user", isBlank(content) ? "" : content.trim(), null, null, null);
        message.setAttachments(attachments);
        conversation.getMessages().add(message);
        if (conversation.getMessages().size() == 1) {
            conversation.setTitle(makeTitle(message));
        }
        conversation.setUpdatedAt(System.currentTimeMillis());
        if (attachmentService != null) {
            attachmentService.bind(id, message.getId(), ids);
        }
        conversationStore.save(conversation);
        return new PreparedChat(toChatRequest(id, conversation), message.getId(), ids);
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
        return toChatRequest(id, conversation);
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

    private AiChatRequest toChatRequest(String conversationId, Conversation conversation) {
        AiChatRequest request = new AiChatRequest();
        request.setProvider(conversation.getProvider());
        request.setModel(conversation.getModel());
        request.setDeepThinking(conversation.getDeepThinking());
        List<AiChatMessageRequest> messages = new ArrayList<>();
        for (ConversationMessage message : conversation.getMessages()) {
            if (!message.getAttachments().isEmpty()) {
                String prompt = isBlank(message.getContent()) ? IMAGE_PROMPT : message.getContent();
                AiChatMessageRequest multimodal = new AiChatMessageRequest(message.getRole(), prompt);
                List<AiChatContentPart> parts = new ArrayList<>();
                parts.add(AiChatContentPart.text(prompt));
                for (ConversationAttachment attachment : message.getAttachments()) {
                    parts.add(AiChatContentPart.image(
                            attachmentService.toDataUrl(conversationId, attachment.getId())));
                }
                multimodal.setContentParts(parts);
                messages.add(multimodal);
            } else if (!isBlank(message.getContent())) {
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
        Conversation conversation = conversationStore.findById(id)
                .orElseThrow(() -> new ParamValiateException("会话不存在或已过期"));
        if (attachmentService != null) {
            attachmentService.enrich(conversation);
        }
        return conversation;
    }

    private void requireProviderAndModel(String provider, String model) {
        if (isBlank(provider) || isBlank(model)) {
            throw new ParamValiateException("provider 和 model 不能为空");
        }
    }

    private String makeTitle(ConversationMessage message) {
        String content = isBlank(message.getContent()) && !message.getAttachments().isEmpty()
                ? "图片：" + message.getAttachments().get(0).getName() : message.getContent();
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() > 26 ? normalized.substring(0, 26) + "…" : normalized;
    }

    private boolean containsImages(Conversation conversation) {
        for (ConversationMessage message : conversation.getMessages()) {
            if (!message.getAttachments().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean supportsImageInput(String provider, String model) {
        if (isBlank(provider) || isBlank(model) || !"openai".equalsIgnoreCase(provider.trim())) {
            return false;
        }
        String normalized = model.trim().toLowerCase();
        return "gpt-4o".equals(normalized) || "gpt-4o-mini".equals(normalized);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static class PreparedChat {
        private final AiChatRequest request;
        private final String userMessageId;
        private final List<String> attachmentIds;

        public PreparedChat(AiChatRequest request, String userMessageId, List<String> attachmentIds) {
            this.request = request;
            this.userMessageId = userMessageId;
            this.attachmentIds = new ArrayList<>(attachmentIds);
        }

        public AiChatRequest getRequest() {
            return request;
        }

        public String getUserMessageId() {
            return userMessageId;
        }

        public List<String> getAttachmentIds() {
            return attachmentIds;
        }
    }
}
