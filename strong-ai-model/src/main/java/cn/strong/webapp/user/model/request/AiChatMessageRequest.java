package cn.strong.webapp.user.model.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import lombok.NoArgsConstructor;

/** 单条对话消息。 */
@Data
@ApiModel("AI对话消息")
@AllArgsConstructor
@NoArgsConstructor
public class AiChatMessageRequest implements Serializable {

    @ApiModelProperty(value = "消息角色", example = "user", required = true)
    private String role;

    @ApiModelProperty(value = "消息内容", example = "请介绍一下你自己", required = true)
    private String content;

    @JsonIgnore
    private List<AiChatContentPart> contentParts;

    public AiChatMessageRequest(String role, String content) {
        this.role = role;
        this.content = content;
    }
}
