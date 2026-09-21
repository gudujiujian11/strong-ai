package cn.strong.webapp.user.conversation.mysql;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationStoreMigrationTest {

    @Test
    void createsConversationAndOrderedMessageTables() throws Exception {
        ClassPathResource migration = new ClassPathResource("db/migration/V4__conversation_store.sql");
        assertTrue(migration.exists(), "conversation store migration must exist");
        String sql;
        try (InputStream input = migration.getInputStream()) {
            sql = StreamUtils.copyToString(input, StandardCharsets.UTF_8);
        }

        assertTrue(sql.contains("CREATE TABLE ai_conversation ("));
        assertTrue(sql.contains("CREATE TABLE ai_conversation_message ("));
        assertTrue(sql.contains("PRIMARY KEY (id)"));
        assertTrue(sql.contains("KEY idx_ai_conversation_updated_at (updated_at)"));
        assertTrue(sql.contains("UNIQUE KEY uk_ai_conversation_message_order (conversation_id, message_order)"));
        assertTrue(sql.contains("KEY idx_ai_conversation_message_conversation (conversation_id)"));
    }
}
