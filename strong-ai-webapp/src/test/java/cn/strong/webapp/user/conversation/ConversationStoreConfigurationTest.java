package cn.strong.webapp.user.conversation;

import cn.strong.webapp.user.conversation.mysql.ConversationMapper;
import cn.strong.webapp.user.conversation.mysql.ConversationMessageMapper;
import cn.strong.webapp.user.conversation.mysql.MysqlConversationStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ConversationStoreConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(StoreTestConfiguration.class);

    @Test
    void shouldUseMysqlByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ConversationStore.class);
            assertThat(context.getBean(ConversationStore.class)).isInstanceOf(MysqlConversationStore.class);
        });
    }

    @Test
    void shouldUseMysqlWhenExplicitlyConfigured() {
        contextRunner.withPropertyValues("strong.conversation.store=mysql").run(context -> {
            assertThat(context).hasSingleBean(ConversationStore.class);
            assertThat(context.getBean(ConversationStore.class)).isInstanceOf(MysqlConversationStore.class);
        });
    }

    @Test
    void shouldUseMemoryOnlyWhenExplicitlyConfigured() {
        contextRunner.withPropertyValues("strong.conversation.store=memory").run(context -> {
            assertThat(context).hasSingleBean(ConversationStore.class);
            assertThat(context.getBean(ConversationStore.class)).isInstanceOf(InMemoryConversationStore.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({InMemoryConversationStore.class, MysqlConversationStore.class})
    static class StoreTestConfiguration {

        @Bean
        ConversationMapper conversationMapper() {
            return mock(ConversationMapper.class);
        }

        @Bean
        ConversationMessageMapper conversationMessageMapper() {
            return mock(ConversationMessageMapper.class);
        }
    }
}
