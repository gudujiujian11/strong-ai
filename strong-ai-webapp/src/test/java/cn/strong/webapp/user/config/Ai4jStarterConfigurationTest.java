package cn.strong.webapp.user.config;

import cn.strong.webapp.user.controller.AiChatController;
import cn.strong.webapp.user.service.AiChatService;
import io.github.lnyocly.ai4j.AiConfigAutoConfiguration;
import io.github.lnyocly.ai4j.platform.openai.chat.OpenAiChatService;
import io.github.lnyocly.ai4j.service.factory.AiServiceRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class Ai4jStarterConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AiConfigAutoConfiguration.class))
            .withPropertyValues(
                    "ai.platforms[0].id=openai",
                    "ai.platforms[0].platform=openai",
                    "ai.platforms[0].api-key=openai-key",
                    "ai.platforms[1].id=doubao",
                    "ai.platforms[1].platform=doubao",
                    "ai.platforms[1].api-key=doubao-key",
                    "ai.platforms[2].id=qwen",
                    "ai.platforms[2].platform=dashscope",
                    "ai.platforms[2].api-key=qwen-key",
                    "ai.platforms[3].id=deepseek",
                    "ai.platforms[3].platform=deepseek",
                    "ai.platforms[3].api-key=deepseek-key");

    @Test
    void shouldCreateRegistryFromAiPlatforms() {
        contextRunner.run(context -> {
            AiServiceRegistry registry = context.getBean(AiServiceRegistry.class);
            assertTrue(registry.contains("openai"));
            assertTrue(registry.getChatService("openai") instanceof OpenAiChatService);
            assertTrue(registry.contains("doubao"));
            assertTrue(registry.contains("qwen"));
            assertTrue(registry.contains("deepseek"));
        });
    }

    @Test
    void shouldCreateChatControllerWithTheApplicationTaskExecutor() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class))
                .withBean(AiChatService.class, () -> mock(AiChatService.class))
                .withBean(AiChatController.class)
                .run(context -> assertTrue(context.containsBean("aiChatController")));
    }
}
