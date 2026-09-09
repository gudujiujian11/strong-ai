package cn.strong.webapp.user.model.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/** 统一 AI 聊天请求。 */
@Data
@ApiModel("AI聊天请求")
public class AiChatRequest implements Serializable {

    @ApiModelProperty(value = "模型厂商: openai、doubao、qwen 或 deepseek", example = "openai", required = true)
    private String provider;

    @ApiModelProperty(value = "模型名/豆包接入点", example = "gpt-5.6-sol", required = true)
    private String model;

    @ApiModelProperty("是否启用模型深度思考")
    private Boolean deepThinking = Boolean.FALSE;

    @ApiModelProperty(value = "上下文消息", required = true)
    private List<AiChatMessageRequest> messages;

    @ApiModelProperty(value = "随机性，范围由模型厂商决定", example = "0.7")
    private Float temperature;

    @ApiModelProperty(value = "核采样参数", example = "0.9")
    private Float topP;

    @ApiModelProperty(value = "最大输出 token 数", example = "1024")
    private Integer maxTokens;

    @ApiModelProperty(value = "本次接口仅支持同步调用，必须为 false", example = "false")
    private Boolean stream = Boolean.FALSE;
}
