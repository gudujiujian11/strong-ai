package cn.strong.webapp.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** AI 厂商连接配置，API Key 建议通过 Nacos 密文或环境变量注入。 */
@Data
@ConfigurationProperties(prefix = "strong.ai")
public class AiModelProperties {

    private Provider doubao = new Provider();
    private Provider qwen = new Provider();
    private Provider deepseek = new Provider();

    @Data
    public static class Provider {
        private String apiKey;
        private String apiHost;
        private String chatCompletionUrl;
        private String responsesUrl;
    }
}
