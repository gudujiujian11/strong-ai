package cn.strong.webapp.user.conversation.mysql;

import cn.strong.webapp.user.conversation.ConversationStore;
import cn.strong.webapp.user.model.conversation.Conversation;
import cn.strong.webapp.user.model.conversation.ConversationMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnProperty(prefix = "strong.conversation", name = "store", havingValue = "mysql", matchIfMissing = true)
public class MysqlConversationStore implements ConversationStore {

    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper messageMapper;

    public MysqlConversationStore(ConversationMapper conversationMapper, ConversationMessageMapper messageMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }

    @Override
    public List<Conversation> findAll() {
        List<ConversationEntity> entities = conversationMapper.findAllOrderByUpdatedAtDesc();
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> conversationIds = entities.stream()
                .map(ConversationEntity::getId)
                .collect(Collectors.toList());
        Map<String, List<ConversationMessageEntity>> messagesByConversation = groupMessages(
                messageMapper.findByConversationIds(conversationIds));
        List<Conversation> result = new ArrayList<>(entities.size());
        for (ConversationEntity entity : entities) {
            result.add(toModel(entity, messagesByConversation.get(entity.getId())));
        }
        return result;
    }

    @Override
    public Optional<Conversation> findById(String id) {
        ConversationEntity entity = conversationMapper.selectById(id);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(toModel(entity, messageMapper.findByConversationId(id)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Conversation save(Conversation conversation) {
        ConversationEntity entity = toEntity(conversation);
        List<ConversationMessageEntity> messages = toEntities(conversation);
        conversationMapper.upsert(entity);
        messageMapper.deleteByConversationId(conversation.getId());
        if (!messages.isEmpty()) {
            messageMapper.insertBatch(messages);
        }
        return toModel(entity, messages);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(String id) {
        if (conversationMapper.deleteById(id) == 0) {
            return false;
        }
        messageMapper.deleteByConversationId(id);
        return true;
    }

    private Map<String, List<ConversationMessageEntity>> groupMessages(List<ConversationMessageEntity> messages) {
        Map<String, List<ConversationMessageEntity>> result = new HashMap<>();
        if (messages == null) {
            return result;
        }
        for (ConversationMessageEntity message : messages) {
            result.computeIfAbsent(message.getConversationId(), ignored -> new ArrayList<>()).add(message);
        }
        return result;
    }

    private Conversation toModel(ConversationEntity entity, List<ConversationMessageEntity> messageEntities) {
        Conversation conversation = new Conversation();
        conversation.setId(entity.getId());
        conversation.setTitle(entity.getTitle());
        conversation.setProvider(entity.getProvider());
        conversation.setModel(entity.getModel());
        conversation.setDeepThinking(entity.getDeepThinking());
        conversation.setCreatedAt(entity.getCreatedAt());
        conversation.setUpdatedAt(entity.getUpdatedAt());
        List<ConversationMessage> messages = new ArrayList<>();
        if (messageEntities != null) {
            for (ConversationMessageEntity messageEntity : messageEntities) {
                messages.add(toModel(messageEntity));
            }
        }
        conversation.setMessages(messages);
        return conversation;
    }

    private ConversationMessage toModel(ConversationMessageEntity entity) {
        ConversationMessage message = new ConversationMessage();
        message.setId(entity.getId());
        message.setRole(entity.getRole());
        message.setContent(entity.getContent());
        message.setReasoning(entity.getReasoning());
        message.setProvider(entity.getProvider());
        message.setModel(entity.getModel());
        message.setPromptTokens(entity.getPromptTokens());
        message.setCompletionTokens(entity.getCompletionTokens());
        message.setTotalTokens(entity.getTotalTokens());
        message.setCreatedAt(entity.getCreatedAt());
        return message;
    }

    private ConversationEntity toEntity(Conversation conversation) {
        ConversationEntity entity = new ConversationEntity();
        entity.setId(conversation.getId());
        entity.setTitle(conversation.getTitle());
        entity.setProvider(conversation.getProvider());
        entity.setModel(conversation.getModel());
        entity.setDeepThinking(conversation.getDeepThinking());
        entity.setCreatedAt(conversation.getCreatedAt());
        entity.setUpdatedAt(conversation.getUpdatedAt());
        return entity;
    }

    private List<ConversationMessageEntity> toEntities(Conversation conversation) {
        List<ConversationMessageEntity> entities = new ArrayList<>(conversation.getMessages().size());
        for (int i = 0; i < conversation.getMessages().size(); i++) {
            ConversationMessage message = conversation.getMessages().get(i);
            ConversationMessageEntity entity = new ConversationMessageEntity();
            entity.setId(message.getId());
            entity.setConversationId(conversation.getId());
            entity.setMessageOrder(i);
            entity.setRole(message.getRole());
            entity.setContent(message.getContent());
            entity.setReasoning(message.getReasoning());
            entity.setProvider(message.getProvider());
            entity.setModel(message.getModel());
            entity.setPromptTokens(message.getPromptTokens());
            entity.setCompletionTokens(message.getCompletionTokens());
            entity.setTotalTokens(message.getTotalTokens());
            entity.setCreatedAt(message.getCreatedAt());
            entities.add(entity);
        }
        return entities;
    }
}
