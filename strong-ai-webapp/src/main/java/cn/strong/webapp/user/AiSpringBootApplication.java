package cn.strong.webapp.user;

import cn.strong.common.utils.PropertiesUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @author <a href="mailto:wf2311@163.com">wf2311</a>
 * @since 2021/6/8 15:39.
 */
@SpringBootApplication
@EnableDiscoveryClient
//todo 扫描feign接口，需要排除自己提供feign接口的包，防止重复生成代理
//@EnableFeignClients(basePackages = {"cn.strong.webapp.user.remote"}) //basePackages是扫描的包路径 用于扫描feign接口
public class AiSpringBootApplication {
    public static void main(String[] args) {
        PropertiesUtil.putServerBootstrapTime();
        SpringApplication.run(AiSpringBootApplication.class, args);
    }
}
