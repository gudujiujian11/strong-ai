package cn.strong.webapp.user.remote;

import cn.strong.common.base.Result;
import cn.strong.webapp.user.remote.api.ExampleRemoteApi;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author <a href="mailto:wf2311@163.com">wf2311</a>
 * @since 2021/6/21 14:45.
 */
@RestController
public class ExampleRemoteService implements ExampleRemoteApi {
    @Override
    public Result<String> test() {
        return Result.ok("test");
    }
}
