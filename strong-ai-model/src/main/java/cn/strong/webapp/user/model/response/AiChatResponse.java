package cn.strong.webapp.user.model.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/** 统一 AI 聊天响应。 */
@Data
@ApiModel("AI聊天响应")
public class AiChatResponse implements Serializable {

    @ApiModelProperty("厂商")
    private String provider;

    @ApiModelProperty("模型")
    private String model;

    @ApiModelProperty("供应商返回的请求 ID")
    private String id;

    @ApiModelProperty("生成内容")
    private String content;

    @ApiModelProperty("结束原因")
    private String finishReason;

    @ApiModelProperty("输入 token 数")
    private Long promptTokens;

    @ApiModelProperty("输出 token 数")
    private Long completionTokens;

    @ApiModelProperty("总 token 数")
    private Long totalTokens;
}
