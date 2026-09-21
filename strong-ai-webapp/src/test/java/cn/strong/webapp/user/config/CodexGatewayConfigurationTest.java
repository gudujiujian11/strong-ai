package cn.strong.webapp.user.config;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodexGatewayConfigurationTest {

    @Test
    void usesDedicatedCodexUpstreamByDefault() throws Exception {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("bootstrap", new ClassPathResource("bootstrap.yml"));
        PropertySource<?> source = sources.get(0);

        assertEquals("codex-deepseek", source.getProperty("ai.platforms[4].id"));
        assertEquals("deepseek", source.getProperty("ai.platforms[4].platform"));
        assertEquals("${CODEX_UPSTREAM_API_HOST:https://api.deepseek.com}",
                source.getProperty("ai.platforms[4].api-host"));
        assertEquals("${CODEX_UPSTREAM_API_KEY:}",
                source.getProperty("ai.platforms[4].api-key"));
        assertEquals("${CODEX_UPSTREAM_PROVIDER:codex-deepseek}",
                source.getProperty("codex.gateway.upstream-provider"));
        assertEquals("${CODEX_UPSTREAM_MODEL:deepseek-chat}",
                source.getProperty("codex.gateway.upstream-model"));
    }
}
