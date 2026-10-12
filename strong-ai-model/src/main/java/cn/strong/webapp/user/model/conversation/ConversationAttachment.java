package cn.strong.webapp.user.model.conversation;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import lombok.Data;

@Data
@ApiModel("会话图片附件")
public class ConversationAttachment implements Serializable {

    @JsonIgnore
    private String conversationId;
    @JsonIgnore
    private String messageId;
    @ApiModelProperty("附件 ID")
    private String id;
    @ApiModelProperty("原始文件名")
    private String name;
    @ApiModelProperty("图片 MIME 类型")
    private String contentType;
    @ApiModelProperty("文件大小，单位字节")
    private long size;
    @ApiModelProperty("附件状态")
    private String status;
    @ApiModelProperty("图片预览地址")
    private String previewUrl;
    @ApiModelProperty("未绑定附件过期时间戳")
    private Long expiresAt;
}
