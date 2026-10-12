package cn.strong.webapp.user.conversation.mysql;

import cn.strong.webapp.user.conversation.ConversationAttachmentData;
import cn.strong.webapp.user.conversation.ConversationAttachmentStore;
import cn.strong.webapp.user.model.conversation.ConversationAttachment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnProperty(prefix = "strong.conversation", name = "store", havingValue = "mysql", matchIfMissing = true)
public class MysqlConversationAttachmentStore implements ConversationAttachmentStore {

    private final ConversationAttachmentMapper mapper;

    public MysqlConversationAttachmentStore(ConversationAttachmentMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversationAttachment save(ConversationAttachment attachment, byte[] content) {
        ConversationAttachmentEntity entity = toEntity(attachment, content);
        mapper.insert(entity);
        return toModel(entity);
    }

    @Override
    public Optional<ConversationAttachmentData> find(String conversationId, String attachmentId) {
        ConversationAttachmentEntity entity = mapper.findWithContent(conversationId, attachmentId);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(new ConversationAttachmentData(toModel(entity), entity.getContent()));
    }

    @Override
    public List<ConversationAttachment> findByConversationIds(List<String> conversationIds) {
        if (conversationIds == null || conversationIds.isEmpty()) {
            return Collections.emptyList();
        }
        return mapper.findByConversationIds(conversationIds).stream()
                .map(this::toModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConversationAttachment> findUnbound(String conversationId, List<String> attachmentIds, long now) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return Collections.emptyList();
        }
        return mapper.findUnbound(conversationId, attachmentIds, now).stream()
                .map(this::toModel)
                .collect(Collectors.toList());
    }

    @Override
    public int bind(String conversationId, String messageId, List<String> attachmentIds, long now) {
        return mapper.bind(conversationId, messageId, attachmentIds, now);
    }

    @Override
    public boolean deleteUnbound(String conversationId, String attachmentId) {
        return mapper.deleteUnbound(conversationId, attachmentId) > 0;
    }

    @Override
    public int deleteByConversationId(String conversationId) {
        return mapper.deleteByConversationId(conversationId);
    }

    @Override
    public int deleteExpired(long now) {
        return mapper.deleteExpired(now);
    }

    private ConversationAttachmentEntity toEntity(ConversationAttachment source, byte[] content) {
        ConversationAttachmentEntity entity = new ConversationAttachmentEntity();
        entity.setId(source.getId());
        entity.setConversationId(source.getConversationId());
        entity.setMessageId(source.getMessageId());
        entity.setOriginalName(source.getName());
        entity.setContentType(source.getContentType());
        entity.setSizeBytes(source.getSize());
        entity.setContent(content);
        entity.setCreatedAt(System.currentTimeMillis());
        entity.setExpiresAt(source.getExpiresAt());
        return entity;
    }

    private ConversationAttachment toModel(ConversationAttachmentEntity source) {
        ConversationAttachment target = new ConversationAttachment();
        target.setId(source.getId());
        target.setConversationId(source.getConversationId());
        target.setMessageId(source.getMessageId());
        target.setName(source.getOriginalName());
        target.setContentType(source.getContentType());
        target.setSize(source.getSizeBytes() == null ? 0L : source.getSizeBytes());
        target.setExpiresAt(source.getExpiresAt());
        target.setStatus(source.getMessageId() == null ? "UPLOADED" : "BOUND");
        return target;
    }
}
