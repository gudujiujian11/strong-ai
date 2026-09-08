package cn.strong.webapp.user.constants;

import cn.strong.common.enums.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author <a href="mailto:wf2311@163.com">wf2311</a>
 * @since 2021/6/15 14:17.
 */
@Getter
@AllArgsConstructor
public enum ExampleCodeEnum implements BaseCode<Integer,String> {
    DEFAULT(0,"默认值")

    ;
    private Integer code;

    private String name;

}
