package cn.strong.webapp.user.model.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author <a href="mailto:wf2311@163.com">wf2311</a>
 * @since 2021/6/22 15:24.
 */
@Data
@ApiModel("示例请求实体类")
public class ExampleRequest implements Serializable {
    @ApiModelProperty("名称")
    private String name;
}
