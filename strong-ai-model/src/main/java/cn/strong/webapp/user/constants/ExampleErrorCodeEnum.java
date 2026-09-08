package cn.strong.webapp.user.constants;

import cn.strong.common.enums.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author <a href="mailto:wf2311@163.com">wf2311</a>
 * @since 2021/6/15 14:18.
 */
@Getter
@AllArgsConstructor
public enum ExampleErrorCodeEnum implements BaseErrorCode {
    DEFAULT(0,"业务异常");
    private int code;

    private String name;
}
