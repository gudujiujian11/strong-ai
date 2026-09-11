package cn.strong.webapp.user.mdoptimizer.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import lombok.Data;

@Data
@ApiModel("Markdown 优化结果")
public class MarkdownOptimizeResponse implements Serializable {

    @ApiModelProperty("任务 ID")
    private String jobId;
    @ApiModelProperty("任务状态")
    private String status;
    @ApiModelProperty("原始内容")
    private String originalContent;
    @ApiModelProperty("优化后内容")
    private String optimizedContent;
    @ApiModelProperty("修改摘要")
    private String changeSummary;
    @ApiModelProperty("风险或校验提示")
    private String warnings;
    @ApiModelProperty("模型厂商")
    private String provider;
    @ApiModelProperty("模型名称")
    private String model;
    @ApiModelProperty("输入 token 数")
    private Long promptTokens;
    @ApiModelProperty("输出 token 数")
    private Long completionTokens;
    @ApiModelProperty("总 token 数")
    private Long totalTokens;
}
