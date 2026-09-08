package cn.strong.webapp.user.config;

import io.github.lnyocly.ai4j.AiConfigAutoConfiguration;
import io.github.lnyocly.ai4j.service.factory.AiServiceRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Ai4jStarterConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AiConfigAutoConfiguration.class))
            .withPropertyValues(
                    "ai.platforms[0].id=doubao",
                    "ai.platforms[0].platform=doubao",
                    "ai.platforms[0].api-key=doubao-key",
                    "ai.platforms[1].id=qwen",
                    "ai.platforms[1].platform=dashscope",
                    "ai.platforms[1].api-key=qwen-key",
                    "ai.platforms[2].id=deepseek",
                    "ai.platforms[2].platform=deepseek",
                    "ai.platforms[2].api-key=deepseek-key");

    @Test
    void shouldCreateRegistryFromAiPlatforms() {
        contextRunner.run(context -> {
            AiServiceRegistry registry = context.getBean(AiServiceRegistry.class);
            assertTrue(registry.contains("doubao"));
            assertTrue(registry.contains("qwen"));
            assertTrue(registry.contains("deepseek"));
        });
    }
}
