package cn.strong.webapp.user.documentgovernance.migration;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentGovernanceMigrationTest {

    @Test
    void addsTeamScopedStrategiesAndNullableTaskReferences() throws Exception {
        ClassPathResource migration = new ClassPathResource("db/migration/V3__document_ai_optimization_strategy.sql");
        assertTrue(migration.exists(), "AI optimization strategy migration must exist");
        String sql;
        try (InputStream input = migration.getInputStream()) {
            sql = StreamUtils.copyToString(input, StandardCharsets.UTF_8);
        }
        assertTrue(sql.contains("CREATE TABLE dg_ai_optimization_strategy ("));
        assertTrue(sql.contains("CREATE TABLE dg_ai_optimization_strategy_version ("));
        assertTrue(sql.contains("UNIQUE KEY uk_dg_ai_strategy_team_name (team_id, name)"));
        assertTrue(sql.contains("UNIQUE KEY uk_dg_ai_strategy_version_no (strategy_id, version_no)"));
        assertTrue(sql.contains("ALTER TABLE dg_ai_task"));
        assertTrue(sql.contains("ADD COLUMN strategy_id BIGINT DEFAULT NULL"));
        assertTrue(sql.contains("ADD COLUMN strategy_version_id BIGINT DEFAULT NULL"));
        assertTrue(sql.contains("ADD COLUMN strategy_config_hash VARCHAR(64) DEFAULT NULL"));
    }
}
