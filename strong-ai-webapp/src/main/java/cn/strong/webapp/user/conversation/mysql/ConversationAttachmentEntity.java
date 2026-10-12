package cn.strong.webapp.user.conversation.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ai_conversation_attachment")
public class ConversationAttachmentEntity {

    @TableId(type = IdType.INPUT)
    private String id;
    private String conversationId;
    private String messageId;
    private String originalName;
    private String contentType;
    private Long sizeBytes;
    private byte[] content;
    private Long createdAt;
    private Long expiresAt;
}
