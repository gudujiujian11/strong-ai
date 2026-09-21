package cn.strong.webapp.user.conversation.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ai_conversation")
public class ConversationEntity {

    @TableId(type = IdType.INPUT)
    private String id;
    private String title;
    private String provider;
    private String model;
    private Boolean deepThinking;
    private Long createdAt;
    private Long updatedAt;
}
