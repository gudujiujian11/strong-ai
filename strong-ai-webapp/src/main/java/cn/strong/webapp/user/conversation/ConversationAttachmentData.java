package cn.strong.webapp.user.conversation;

import cn.strong.webapp.user.model.conversation.ConversationAttachment;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConversationAttachmentData {

    private ConversationAttachment metadata;
    private byte[] content;
}
