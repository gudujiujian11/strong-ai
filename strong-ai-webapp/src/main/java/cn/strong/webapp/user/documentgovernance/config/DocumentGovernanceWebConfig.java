package cn.strong.webapp.user.documentgovernance.config;

import cn.strong.webapp.user.documentgovernance.context.DocumentIdentityInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableScheduling
public class DocumentGovernanceWebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new DocumentIdentityInterceptor())
                .addPathPatterns("/api/document-governance/**");
    }
}
