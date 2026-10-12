package cn.strong.webapp.user.conversation;

import cn.strong.webapp.user.model.conversation.ConversationAttachment;
import java.util.List;
import java.util.Optional;

public interface ConversationAttachmentStore {

    ConversationAttachment save(ConversationAttachment attachment, byte[] content);

    Optional<ConversationAttachmentData> find(String conversationId, String attachmentId);

    List<ConversationAttachment> findByConversationIds(List<String> conversationIds);

    List<ConversationAttachment> findUnbound(String conversationId, List<String> attachmentIds, long now);

    int bind(String conversationId, String messageId, List<String> attachmentIds, long now);

    boolean deleteUnbound(String conversationId, String attachmentId);

    int deleteByConversationId(String conversationId);

    int deleteExpired(long now);
}
