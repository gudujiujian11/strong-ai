package cn.strong.webapp.user.mdoptimizer.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import lombok.Data;

@Data
@ApiModel("Markdown 优化请求")
public class MarkdownOptimizeRequest implements Serializable {

    @ApiModelProperty("输入来源：file 或 text")
    private String sourceType = "text";
    @ApiModelProperty("Markdown 文本")
    private String content;
    @ApiModelProperty("原始文件名")
    private String fileName;
    @ApiModelProperty("优化模式：format、polish 或 restructure")
    private String mode = "format";
    @ApiModelProperty("文档类型")
    private String documentType = "通用 Markdown 文档";
    @ApiModelProperty("目标读者")
    private String audience = "通用读者";
    @ApiModelProperty("输出语气")
    private String tone = "专业、简洁";
    @ApiModelProperty("是否保护代码块")
    private Boolean preserveCode = Boolean.TRUE;
    @ApiModelProperty("是否保护链接")
    private Boolean preserveLinks = Boolean.TRUE;
    @ApiModelProperty("模型厂商")
    private String provider = "openai";
    @ApiModelProperty("模型名称")
    private String model = "gpt-5.6-sol";
    @ApiModelProperty("是否启用深度思考")
    private Boolean deepThinking = Boolean.FALSE;
    @ApiModelProperty("最大输出 token 数")
    private Integer maxTokens = 8192;
}
