package cn.strong.webapp.user.model.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import lombok.Data;

@Data
@ApiModel("更新会话请求")
public class ConversationUpdateRequest implements Serializable {

    @ApiModelProperty(value = "模型厂商", example = "openai")
    private String provider;
    @ApiModelProperty(value = "模型名称", example = "gpt-5.6-sol")
    private String model;

    @ApiModelProperty("是否启用深度思考")
    private Boolean deepThinking;
}
