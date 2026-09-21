package cn.strong.webapp.user.conversation.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ai_conversation_message")
public class ConversationMessageEntity {

    @TableId(type = IdType.INPUT)
    private String id;
    private String conversationId;
    private Integer messageOrder;
    private String role;
    private String content;
    private String reasoning;
    private String provider;
    private String model;
    private Long promptTokens;
    private Long completionTokens;
    private Long totalTokens;
    private Long createdAt;
}
