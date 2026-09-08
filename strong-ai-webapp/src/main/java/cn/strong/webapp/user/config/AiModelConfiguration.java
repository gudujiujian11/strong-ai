package cn.strong.webapp.user.config;

import io.github.lnyocly.ai4j.config.DashScopeConfig;
import io.github.lnyocly.ai4j.config.DeepSeekConfig;
import io.github.lnyocly.ai4j.config.DoubaoConfig;
import io.github.lnyocly.ai4j.service.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/** 创建 ai4j 的厂商配置对象。 */
@org.springframework.context.annotation.Configuration
@EnableConfigurationProperties(AiModelProperties.class)
public class AiModelConfiguration {

    @Bean
    public Configuration ai4jConfiguration(AiModelProperties properties) {
        Configuration configuration = new Configuration();
        AiModelProperties.Provider doubao = properties.getDoubao();
        DoubaoConfig doubaoConfig = new DoubaoConfig();
        doubaoConfig.setApiKey(doubao.getApiKey());
        doubaoConfig.setApiHost(defaultValue(doubao.getApiHost(), "https://ark.cn-beijing.volces.com/api/v3"));
        doubaoConfig.setChatCompletionUrl(defaultValue(doubao.getChatCompletionUrl(), "/chat/completions"));
        doubaoConfig.setResponsesUrl(doubao.getResponsesUrl());
        configuration.setDoubaoConfig(doubaoConfig);

        AiModelProperties.Provider qwen = properties.getQwen();
        DashScopeConfig qwenConfig = new DashScopeConfig();
        qwenConfig.setApiKey(qwen.getApiKey());
        qwenConfig.setApiHost(defaultValue(qwen.getApiHost(), "https://dashscope.aliyuncs.com/api/v1"));
        qwenConfig.setResponsesUrl(defaultValue(qwen.getResponsesUrl(), "/services/aigc/text-generation/generation"));
        configuration.setDashScopeConfig(qwenConfig);

        AiModelProperties.Provider deepseek = properties.getDeepseek();
        DeepSeekConfig deepSeekConfig = new DeepSeekConfig();
        deepSeekConfig.setApiKey(deepseek.getApiKey());
        deepSeekConfig.setApiHost(defaultValue(deepseek.getApiHost(), "https://api.deepseek.com"));
        deepSeekConfig.setChatCompletionUrl(defaultValue(deepseek.getChatCompletionUrl(), "/chat/completions"));
        configuration.setDeepSeekConfig(deepSeekConfig);
        return configuration;
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
