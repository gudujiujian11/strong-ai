# Task 1 实施报告

状态：完成；未创建 commit、push 或 PR。

## 变更

- 新增 V3 迁移：策略主表、版本表（均含 team_id），团队名称唯一键、策略版本号唯一键和查询索引。
- dg_ai_task 新增可空 strategy_id、strategy_version_id、strategy_config_hash，以及带 team_id 的策略、版本索引；没有修改旧字段定义。
- 手工 SQL 同步策略表和任务增量，并按主任务补充指示补齐既有 created_by/result_summary 字段，修复其与 V1 的历史漂移。
- 新增配置 DTO、方向/强度枚举、编译结果上下文、请求/响应 DTO。defaults() 按设计 §6.4 构造独立配置对象；使用 Java 8 兼容语法。
- 配置结构：Map<String, DirectionConfig> directions；嵌套 Protections、Constraints、ModelConfig；DirectionConfig.intensity 使用 AiOptimizationIntensity。枚举 getCode() 以 @JsonValue 输出设计要求的编码。
- 编译结果上下文提供 config/configHash/systemPrompt 三参构造及 getter；严格反序列化、校验、规范化与编译留给 Task 2。
- 新增策略/版本实体与 Mapper；所有新增自定义查询显式过滤 team_id。
- defaultStrategy 使用 @TableField("is_default")，自定义查询使用 @Results/@ResultMap 显式映射同一字段。
- AiOptimizeRequest 增加 strategyId；AiTaskResponse 增加 strategyId、strategyVersionId、strategyVersionNo、strategyConfigHash；AiTaskEntity 增加三个持久化字段。AiTaskMapper 改用显式查询列列表，包含三个新增字段。
- 按主任务补充指示未加入生成列或默认策略数据库唯一约束；单默认策略由 Task 3 的同事务清除旧默认、设置新默认负责。

## RED / GREEN 与编译

RED（先仅新增测试，尚未创建 V3）：

```bash
MAVEN_OPTS='-Dfile.encoding=UTF-8 -DforkCount=0 -DfailIfNoTests=false' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -am -Dtest=DocumentGovernanceMigrationTest test
```

退出码 1；关键输出：

```text
Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
AI optimization strategy migration must exist ==> expected: <true> but was: <false>
BUILD FAILURE
```

原因符合预期：迁移资源尚不存在，没有连接生产数据库，也不是编译错误。

实现后的编译：

```bash
MAVEN_OPTS='-Dfile.encoding=UTF-8 -DforkCount=0 -DfailIfNoTests=false' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -am -DskipTests compile
```

退出码 0；全部五个 reactor 模块 SUCCESS；BUILD SUCCESS，23.145 s。

GREEN：再次执行与 RED 完全相同的聚焦测试命令。

退出码 0；关键输出：

```text
Running cn.strong.webapp.user.documentgovernance.migration.DocumentGovernanceMigrationTest
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time: 28.169 s
```

测试按 brief 使用 ClassPathResource、UTF-8 与 assertTrue(sql.contains(...))，覆盖两张新表、两个唯一键和三个可空任务字段。

## 自检

- git diff --check：退出码 0，无空白问题。
- diff 比对 V3 与手工 SQL 中两张策略表：退出码 0，文本完全一致。
- 使用 Ruby 只读脚本提取 CREATE TABLE 内容，去掉注释后按字段/索引定义比较手工任务表与 V1 + V3：退出码 0，输出 `PASS: strategy tables identical; dg_ai_task matches V1 + V3 by column/index definition`。
- 上述 Ruby 检查首次受当前 shell 默认 US-ASCII 编码影响而失败，补充 `ruby -EUTF-8` 后通过；没有因此修改源码。
- 核查新增策略 Mapper 的全部 SELECT 均带 team_id；版本表也持久化 team_id，便于直接租户过滤。
- 原 AiTaskMapper.deleteExpiredTerminalTasks 是既有跨团队维护删除，本任务未改动。
- 保留用户已有 DocumentServiceTest 的 Java 8 修复、未跟踪设计与计划文档，没有回滚或覆盖。

## 文件

新增：

- strong-ai-webapp/src/main/resources/db/migration/V3__document_ai_optimization_strategy.sql
- strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/AiOptimizationStrategyConfig.java
- strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/AiOptimizationStrategyCompiledContext.java
- strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/AiOptimizationDirection.java
- strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/AiOptimizationIntensity.java
- strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/AiOptimizationStrategyRequest.java
- strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/AiOptimizationStrategyResponse.java
- strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/AiOptimizationStrategyVersionResponse.java
- strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/entity/AiOptimizationStrategyEntity.java
- strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/entity/AiOptimizationStrategyVersionEntity.java
- strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/mapper/AiOptimizationStrategyMapper.java
- strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/mapper/AiOptimizationStrategyVersionMapper.java
- strong-ai-webapp/src/test/java/cn/strong/webapp/user/documentgovernance/migration/DocumentGovernanceMigrationTest.java

修改：

- doc/sql/document-governance-mysql.sql
- strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/AiOptimizeRequest.java
- strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/AiTaskResponse.java
- strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/entity/AiTaskEntity.java
- strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/mapper/AiTaskMapper.java

## 关注点 / 未验证项

- 没有连接 MySQL 运行实际迁移，也未执行 Mapper 数据库集成测试；本次验证为编译、迁移资源契约与脚本一致性检查。
- 单默认策略的并发安全需 Task 3 保证，包括在事务内可靠串行化同团队设置默认操作。
- selectPublishedByStrategyId(teamId, strategyId) 返回 List<AiOptimizationStrategyVersionEntity>，按 version_no DESC 返回全部已发布版本；后续服务选择当前版本应结合主表 currentVersionId。
- 旧任务的新增字段保持 null；本任务未实现服务层快照写入和响应字段填充。
- 构建存在原有父 POM 插件版本缺失、已弃用 API 与不 fork 时 useSystemClassloader 无效的告警，未改动这些范围外配置。
