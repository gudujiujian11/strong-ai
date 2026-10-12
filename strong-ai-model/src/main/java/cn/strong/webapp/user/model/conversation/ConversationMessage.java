package cn.strong.webapp.user.model.conversation;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
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
    @ApiModelProperty("输入 token 数")
    private Long promptTokens;
    @ApiModelProperty("输出 token 数")
    private Long completionTokens;
    @ApiModelProperty("总 token 数")
    private Long totalTokens;
    @ApiModelProperty("创建时间戳")
    private long createdAt;
    @ApiModelProperty("消息图片附件")
    private List<ConversationAttachment> attachments = new ArrayList<>();
}
