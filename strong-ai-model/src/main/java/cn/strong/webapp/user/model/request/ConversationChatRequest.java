package cn.strong.webapp.user.model.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import lombok.Data;

@Data
@ApiModel("发送会话消息请求")
public class ConversationChatRequest implements Serializable {

    @ApiModelProperty(value = "用户输入内容", required = true)
    private String content;
}
