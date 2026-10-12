package cn.strong.webapp.user.conversation;

import cn.strong.webapp.user.model.conversation.ConversationAttachment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "strong.conversation", name = "store", havingValue = "memory")
public class InMemoryConversationAttachmentStore implements ConversationAttachmentStore {

    private final Map<String, StoredAttachment> attachments = new ConcurrentHashMap<>();

    @Override
    public ConversationAttachment save(ConversationAttachment attachment, byte[] content) {
        ConversationAttachment snapshot = copy(attachment);
        attachments.put(snapshot.getId(), new StoredAttachment(snapshot, content.clone()));
        return copy(snapshot);
    }

    @Override
    public Optional<ConversationAttachmentData> find(String conversationId, String attachmentId) {
        StoredAttachment stored = attachments.get(attachmentId);
        if (stored == null || !conversationId.equals(stored.metadata.getConversationId())) {
            return Optional.empty();
        }
        return Optional.of(new ConversationAttachmentData(copy(stored.metadata), stored.content.clone()));
    }

    @Override
    public List<ConversationAttachment> findByConversationIds(List<String> conversationIds) {
        if (conversationIds == null || conversationIds.isEmpty()) {
            return Collections.emptyList();
        }
        return attachments.values().stream()
                .filter(item -> conversationIds.contains(item.metadata.getConversationId()))
                .map(item -> copy(item.metadata))
                .collect(Collectors.toList());
    }

    @Override
    public List<ConversationAttachment> findUnbound(String conversationId, List<String> attachmentIds, long now) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<ConversationAttachment> result = new ArrayList<>();
        for (String attachmentId : attachmentIds) {
            StoredAttachment stored = attachments.get(attachmentId);
            if (stored != null && conversationId.equals(stored.metadata.getConversationId())
                    && stored.metadata.getMessageId() == null
                    && (stored.metadata.getExpiresAt() == null || stored.metadata.getExpiresAt() > now)) {
                result.add(copy(stored.metadata));
            }
        }
        return result;
    }

    @Override
    public synchronized int bind(String conversationId, String messageId, List<String> attachmentIds, long now) {
        List<ConversationAttachment> available = findUnbound(conversationId, attachmentIds, now);
        if (available.size() != attachmentIds.size()) {
            return 0;
        }
        for (String attachmentId : attachmentIds) {
            StoredAttachment stored = attachments.get(attachmentId);
            stored.metadata.setMessageId(messageId);
            stored.metadata.setExpiresAt(null);
        }
        return attachmentIds.size();
    }

    @Override
    public boolean deleteUnbound(String conversationId, String attachmentId) {
        StoredAttachment stored = attachments.get(attachmentId);
        if (stored == null || !conversationId.equals(stored.metadata.getConversationId())
                || stored.metadata.getMessageId() != null) {
            return false;
        }
        return attachments.remove(attachmentId, stored);
    }

    @Override
    public int deleteByConversationId(String conversationId) {
        int count = 0;
        for (String attachmentId : new ArrayList<>(attachments.keySet())) {
            StoredAttachment stored = attachments.get(attachmentId);
            if (stored != null && conversationId.equals(stored.metadata.getConversationId())
                    && attachments.remove(attachmentId, stored)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int deleteExpired(long now) {
        int count = 0;
        for (String attachmentId : new ArrayList<>(attachments.keySet())) {
            StoredAttachment stored = attachments.get(attachmentId);
            if (stored != null && stored.metadata.getMessageId() == null
                    && stored.metadata.getExpiresAt() != null && stored.metadata.getExpiresAt() <= now
                    && attachments.remove(attachmentId, stored)) {
                count++;
            }
        }
        return count;
    }

    private ConversationAttachment copy(ConversationAttachment source) {
        ConversationAttachment target = new ConversationAttachment();
        target.setId(source.getId());
        target.setConversationId(source.getConversationId());
        target.setMessageId(source.getMessageId());
        target.setName(source.getName());
        target.setContentType(source.getContentType());
        target.setSize(source.getSize());
        target.setStatus(source.getStatus());
        target.setPreviewUrl(source.getPreviewUrl());
        target.setExpiresAt(source.getExpiresAt());
        return target;
    }

    private static class StoredAttachment {
        private final ConversationAttachment metadata;
        private final byte[] content;

        private StoredAttachment(ConversationAttachment metadata, byte[] content) {
            this.metadata = metadata;
            this.content = content;
        }
    }
}
