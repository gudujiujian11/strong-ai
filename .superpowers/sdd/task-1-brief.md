### Task 1: 创建数据库迁移和持久化模型

**Files:**
- Create: `strong-ai-webapp/src/main/resources/db/migration/V3__document_ai_optimization_strategy.sql`
- Modify: `doc/sql/document-governance-mysql.sql`
- Create: `strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/AiOptimizationStrategyConfig.java`
- Create: `strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/AiOptimizationStrategyCompiledContext.java`
- Create: `strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/AiOptimizationDirection.java`
- Create: `strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/AiOptimizationIntensity.java`
- Create: `strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/AiOptimizationStrategyRequest.java`
- Create: `strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/AiOptimizationStrategyResponse.java`
- Create: `strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/AiOptimizationStrategyVersionResponse.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/entity/AiOptimizationStrategyEntity.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/entity/AiOptimizationStrategyVersionEntity.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/mapper/AiOptimizationStrategyMapper.java`
- Create: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/mapper/AiOptimizationStrategyVersionMapper.java`
- Modify: `strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/AiOptimizeRequest.java`
- Modify: `strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/AiTaskResponse.java`
- Modify: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/entity/AiTaskEntity.java`
- Modify: `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/mapper/AiTaskMapper.java`
- Create: `strong-ai-webapp/src/test/java/cn/strong/webapp/user/documentgovernance/migration/DocumentGovernanceMigrationTest.java`

**Interfaces:**
- Produces `AiOptimizationStrategyConfig` with `directions`, `protections`, `constraints`, `customInstruction` and `model` fields.
- Produces `AiOptimizationStrategyEntity`/`AiOptimizationStrategyVersionEntity` mapped to `dg_ai_optimization_strategy` and `dg_ai_optimization_strategy_version`.
- Produces `AiOptimizeRequest.getStrategyId()` and `AiTaskResponse.getStrategyVersionId()`.

- [ ] **Step 1: Write the migration assertions**

Create `DocumentGovernanceMigrationTest` to load `V3__document_ai_optimization_strategy.sql` and assert that it contains both new tables, unique keys on `(team_id, name)` and `(strategy_id, version_no)`, and nullable `strategy_id`, `strategy_version_id`, `strategy_config_hash` columns on `dg_ai_task`.

The test must use `ClassPathResource("db/migration/V3__document_ai_optimization_strategy.sql")`, read UTF-8 text, and assert with `assertTrue(sql.contains(...))`; it must not connect to a production database.

- [ ] **Step 2: Run the focused migration test**

Run:

```bash
MAVEN_OPTS='-Dfile.encoding=UTF-8 -DforkCount=0 -DfailIfNoTests=false' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -am -Dtest=DocumentGovernanceMigrationTest test
```

Expected: `DocumentGovernanceMigrationTest` fails before the migration is added.

- [ ] **Step 3: Add `V3` schema and synchronized manual SQL**

Create the two tables with `team_id`, status, version, config JSON, config hash, audit timestamps and indexes. Add the three nullable task columns and indexes. Keep the existing `dg_ai_task` column semantics unchanged.

- [ ] **Step 4: Add Java model and mapper types**

Use the repository pattern:

```java
@Data
@TableName("dg_ai_optimization_strategy")
public class AiOptimizationStrategyEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private String teamId;
    private String name;
    private String description;
    private String status;
    private Long currentVersionId;
    private Boolean defaultStrategy;
    private String createdBy;
    private Long createdAt;
    private Long updatedAt;
}
```

Add mapper methods `selectByTeamId`, `selectByTeamAndId`, `selectByTeamAndName`, and version methods `selectByStrategyId`, `selectByStrategyAndVersionNo`, `selectPublishedByStrategyId` with explicit team filters.

`AiOptimizationStrategyCompiledContext` must expose `getConfig()`, `getConfigHash()` and `getSystemPrompt()` and have a constructor accepting those three values.

- [ ] **Step 5: Run compile and inspect schema consistency**

Run:

```bash
MAVEN_OPTS='-Dfile.encoding=UTF-8 -DforkCount=0 -DfailIfNoTests=false' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -am -DskipTests compile
```

Expected: exit code 0; `V3` and `doc/sql/document-governance-mysql.sql` contain the same strategy tables and task columns.

