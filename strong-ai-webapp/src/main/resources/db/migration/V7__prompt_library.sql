CREATE TABLE ai_prompt (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Prompt ID',
  category VARCHAR(32) NOT NULL COMMENT 'Prompt category',
  title VARCHAR(100) NOT NULL COMMENT 'Prompt title',
  description VARCHAR(500) NOT NULL DEFAULT '' COMMENT 'Prompt description',
  tags VARCHAR(500) NOT NULL DEFAULT '' COMMENT 'Comma-separated tags',
  content MEDIUMTEXT NOT NULL COMMENT 'Current working copy',
  current_version_no BIGINT NOT NULL DEFAULT 0 COMMENT 'Latest published version number',
  rating TINYINT NOT NULL DEFAULT 0 COMMENT 'Global rating from 0 to 5',
  favorite_count INT NOT NULL DEFAULT 0 COMMENT 'Global favorite count',
  created_at BIGINT NOT NULL,
  updated_at BIGINT NOT NULL,
  PRIMARY KEY (id),
  KEY idx_ai_prompt_category (category, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Prompt library';

CREATE TABLE ai_prompt_version (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Prompt version ID',
  prompt_id BIGINT NOT NULL COMMENT 'Prompt ID',
  version_no BIGINT NOT NULL COMMENT 'Version number',
  content MEDIUMTEXT NOT NULL COMMENT 'Published prompt content',
  created_at BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_prompt_version (prompt_id, version_no),
  CONSTRAINT fk_ai_prompt_version_prompt FOREIGN KEY (prompt_id) REFERENCES ai_prompt (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Prompt library versions';

INSERT INTO ai_prompt (category, title, description, tags, content, current_version_no, rating, favorite_count, created_at, updated_at)
VALUES
('JAVA_REVIEW', 'Java 代码审查清单', '从正确性、可维护性和安全性检查一段 Java 代码。', 'Java,代码审查,质量',
'请审查下面的 Java 代码，重点关注：\n1. 逻辑正确性与边界条件\n2. 异常处理、资源释放和并发安全\n3. 可读性、可维护性和性能\n4. 潜在安全风险\n\n请按“问题 / 严重程度 / 原因 / 修改建议”输出，最后给出整体结论。\n\n代码：\n```java\n{{代码}}\n```', 1, 0, 0, UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000, UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000),
('JAVA_REVIEW', 'Spring 服务重构建议', '分析 Spring Service 的职责边界和依赖设计。', 'Java,Spring,重构',
'请分析下面的 Spring Service，指出职责过多、依赖耦合、事务边界和可测试性问题，并给出分阶段重构方案。\n\nService 代码：\n```java\n{{Service代码}}\n```', 1, 0, 0, UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000, UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000),
('SQL_OPTIMIZE', 'SQL 性能优化分析', '结合表结构和执行计划定位 SQL 慢查询原因。', 'SQL,性能,索引',
'请分析下面的 SQL 慢查询，结合表结构和执行计划说明瓶颈，并给出可落地的优化 SQL、索引建议及风险。\n\n表结构：\n```sql\n{{表结构}}\n```\n\nSQL：\n```sql\n{{SQL}}\n```\n\n执行计划：\n```text\n{{执行计划}}\n```', 1, 0, 0, UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000, UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000),
('SQL_OPTIMIZE', '数据库变更评审', '评估数据库 DDL 和迁移脚本的兼容性与风险。', 'SQL,DDL,迁移',
'请评审下面的数据库变更脚本，检查锁表、数据兼容、回滚、索引和上线顺序风险，并输出上线前检查清单。\n\n变更脚本：\n```sql\n{{迁移脚本}}\n```', 1, 0, 0, UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000, UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000),
('DOC_WRITING', '技术方案文档', '把零散需求整理为可评审的技术方案。', '文档,方案,架构',
'请将下面的需求整理成技术方案，包含：背景与目标、范围与非目标、方案概述、核心流程、数据模型、接口设计、异常处理、上线计划和风险。\n\n需求：\n{{需求描述}}', 1, 0, 0, UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000, UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000),
('DOC_WRITING', '会议纪要与行动项', '将会议记录整理成清晰的结论和待办。', '文档,会议,行动项',
'请把下面的会议记录整理成：会议结论、关键分歧、已确认决策、待办事项（负责人 / 截止时间 / 交付物）和待确认问题。不要补造记录中没有的信息。\n\n会议记录：\n{{会议记录}}', 1, 0, 0, UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000, UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000),
('TROUBLESHOOT', '线上故障排查助手', '从现象、日志和变更记录推导排查路径。', '故障,排查,线上',
'请协助排查下面的线上故障。先总结现象和影响，再按优先级给出假设、验证命令或指标、临时止血方案、根因定位路径和长期修复建议。不要把未经验证的假设当成结论。\n\n现象：\n{{故障现象}}\n\n日志或监控：\n```text\n{{日志与监控}}\n```\n\n最近变更：\n{{最近变更}}', 1, 0, 0, UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000, UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000),
('TROUBLESHOOT', '接口错误分析', '快速拆解接口错误响应和上下游影响。', '故障,接口,分析',
'请分析下面的接口错误，输出：错误含义、最可能的原因、需要补充的信息、客户端与服务端分别应采取的措施，以及避免复发的监控和测试建议。\n\n请求信息：\n{{请求信息}}\n\n错误响应：\n```json\n{{错误响应}}\n```', 1, 0, 0, UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000, UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000);

INSERT INTO ai_prompt_version (prompt_id, version_no, content, created_at)
SELECT id, 1, content, created_at FROM ai_prompt;
