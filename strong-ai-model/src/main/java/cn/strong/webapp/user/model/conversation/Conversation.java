package cn.strong.webapp.user.model.conversation;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
@ApiModel("聊天会话")
public class Conversation implements Serializable {

    @ApiModelProperty("会话 ID")
    private String id;
    @ApiModelProperty("会话标题")
    private String title;
    @ApiModelProperty("模型厂商")
    private String provider;
    @ApiModelProperty("模型名称")
    private String model;
    @ApiModelProperty("是否启用深度思考")
    private Boolean deepThinking = Boolean.FALSE;
    @ApiModelProperty("会话消息")
    private List<ConversationMessage> messages = new ArrayList<>();
    @ApiModelProperty("创建时间戳")
    private long createdAt;
    @ApiModelProperty("更新时间戳")
    private long updatedAt;
}
