package cn.strong.webapp.user.remote.api;

import cn.strong.common.base.Result;
import cn.strong.common.constants.ApiCsts;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * ExampleRemoteApi
 * @author wys
 * @since 2023/04/15 10:19.
 */
@FeignClient(name = "strong-ai") // name是服务名 即 spring.application.name
@RequestMapping(ApiCsts.FEIGN_API_PREFIX) //路径前缀 暂定 inner/xxx  xxx为模块名 无需鉴权 网关层禁用此路径外部访问/特定内部ip访问
public interface ExampleRemoteApi {

    @GetMapping("/test")
    Result<String> test();

}

