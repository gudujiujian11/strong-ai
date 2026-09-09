package cn.strong.webapp.user.model.conversation;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import lombok.Data;

@Data
@ApiModel("会话消息")
public class ConversationMessage implements Serializable {

    @ApiModelProperty("消息 ID")
    private String id;
    @ApiModelProperty(value = "消息角色", example = "user")
    private String role;
    @ApiModelProperty("消息内容")
    private String content;
    @ApiModelProperty("思考内容")
    private String reasoning;
    @ApiModelProperty("生成消息的模型厂商")
    private String provider;
    @ApiModelProperty("生成消息的模型")
    private String model;
    @ApiModelProperty("创建时间戳")
    private long createdAt;
}
