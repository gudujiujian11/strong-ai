-- 技术文档治理台 MySQL 建表脚本。
-- 策略表与任务策略字段同步自 V3__document_ai_optimization_strategy.sql。
-- 时间字段统一存储 Unix 毫秒时间戳；执行前请确保目标库使用 utf8mb4。

CREATE TABLE dg_document (
  id BIGINT NOT NULL AUTO_INCREMENT,
  team_id VARCHAR(64) NOT NULL,
  owner_user_id VARCHAR(64) NOT NULL,
  title VARCHAR(200) NOT NULL,
  document_type VARCHAR(64) NOT NULL,
  tags VARCHAR(500) NOT NULL DEFAULT '',
  status VARCHAR(16) NOT NULL,
  draft_content MEDIUMTEXT NOT NULL,
  current_version_id BIGINT DEFAULT NULL,
  created_at BIGINT NOT NULL,
  updated_at BIGINT NOT NULL,
  PRIMARY KEY (id),
  KEY idx_dg_document_team_updated (team_id, updated_at),
  KEY idx_dg_document_team_owner (team_id, owner_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE dg_document_version (
  id BIGINT NOT NULL AUTO_INCREMENT,
  document_id BIGINT NOT NULL,
  team_id VARCHAR(64) NOT NULL,
  version_no BIGINT NOT NULL,
  content MEDIUMTEXT NOT NULL,
  diff_content MEDIUMTEXT NOT NULL,
  release_note VARCHAR(500) NOT NULL DEFAULT '',
  created_by VARCHAR(64) NOT NULL,
  created_at BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_dg_version_document_no (document_id, version_no),
  KEY idx_dg_version_team_document (team_id, document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE dg_template (
  id BIGINT NOT NULL AUTO_INCREMENT,
  team_id VARCHAR(64) NOT NULL,
  name VARCHAR(100) NOT NULL,
  document_type VARCHAR(64) NOT NULL,
  required_headings TEXT NOT NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at BIGINT NOT NULL,
  updated_at BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_dg_template_team_name (team_id, name),
  KEY idx_dg_template_team_type (team_id, document_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE dg_term (
  id BIGINT NOT NULL AUTO_INCREMENT,
  team_id VARCHAR(64) NOT NULL,
  term VARCHAR(200) NOT NULL,
  preferred_term VARCHAR(200) NOT NULL DEFAULT '',
  is_forbidden TINYINT(1) NOT NULL DEFAULT 0,
  description VARCHAR(500) NOT NULL DEFAULT '',
  created_by VARCHAR(64) NOT NULL,
  created_at BIGINT NOT NULL,
  updated_at BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_dg_term_team_term (team_id, term),
  KEY idx_dg_term_team (team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE dg_rule_set (
  id BIGINT NOT NULL AUTO_INCREMENT,
  team_id VARCHAR(64) NOT NULL,
  name VARCHAR(100) NOT NULL,
  config_json TEXT NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(64) NOT NULL,
  created_at BIGINT NOT NULL,
  updated_at BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_dg_rule_set_team_name (team_id, name),
  KEY idx_dg_rule_set_team (team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE dg_quality_report (
  id BIGINT NOT NULL AUTO_INCREMENT,
  team_id VARCHAR(64) NOT NULL,
  document_id BIGINT NOT NULL,
  document_version_id BIGINT DEFAULT NULL,
  score INT NOT NULL,
  checked_by VARCHAR(64) NOT NULL,
  created_at BIGINT NOT NULL,
  PRIMARY KEY (id),
  KEY idx_dg_report_team_document (team_id, document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE dg_quality_issue (
  id BIGINT NOT NULL AUTO_INCREMENT,
  team_id VARCHAR(64) NOT NULL,
  report_id BIGINT NOT NULL,
  rule_code VARCHAR(64) NOT NULL,
  severity VARCHAR(16) NOT NULL,
  line_number INT NOT NULL,
  message VARCHAR(500) NOT NULL,
  suggestion VARCHAR(500) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_dg_issue_team_report (team_id, report_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE dg_ai_task (
  id BIGINT NOT NULL AUTO_INCREMENT,
  team_id VARCHAR(64) NOT NULL,
  document_id BIGINT NOT NULL,
  idempotency_key VARCHAR(100) NOT NULL,
  strategy_id BIGINT DEFAULT NULL COMMENT '提交时选择的策略 ID',
  strategy_version_id BIGINT DEFAULT NULL COMMENT '执行策略版本 ID',
  strategy_config_hash VARCHAR(64) DEFAULT NULL COMMENT '执行策略配置哈希',
  created_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '任务创建用户 ID',
  status VARCHAR(16) NOT NULL,
  provider VARCHAR(64) NOT NULL,
  model VARCHAR(100) NOT NULL,
  result_summary VARCHAR(500) NOT NULL DEFAULT '' COMMENT '任务结果摘要',
  error_code VARCHAR(64) NOT NULL DEFAULT '',
  prompt_tokens BIGINT DEFAULT NULL,
  completion_tokens BIGINT DEFAULT NULL,
  total_tokens BIGINT DEFAULT NULL,
  created_at BIGINT NOT NULL,
  updated_at BIGINT NOT NULL,
  expires_at BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_dg_task_idempotency (team_id, document_id, idempotency_key),
  KEY idx_dg_task_team_expiry (team_id, expires_at),
  KEY idx_dg_task_team_strategy (team_id, strategy_id),
  KEY idx_dg_task_team_strategy_version (team_id, strategy_version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE dg_ai_optimization_strategy (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  team_id VARCHAR(64) NOT NULL COMMENT '团队 ID',
  name VARCHAR(100) NOT NULL COMMENT '策略名称',
  description VARCHAR(500) NOT NULL DEFAULT '' COMMENT '策略说明',
  status VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT '策略状态：ENABLED / DISABLED',
  current_version_id BIGINT DEFAULT NULL COMMENT '当前发布版本 ID',
  is_default TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否团队默认策略',
  created_by VARCHAR(64) NOT NULL COMMENT '创建用户 ID',
  created_at BIGINT NOT NULL COMMENT '创建时间戳（毫秒）',
  updated_at BIGINT NOT NULL COMMENT '更新时间戳（毫秒）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dg_ai_strategy_team_name (team_id, name),
  KEY idx_dg_ai_strategy_team_status (team_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 优化策略表';

CREATE TABLE dg_ai_optimization_strategy_version (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  team_id VARCHAR(64) NOT NULL COMMENT '团队 ID',
  strategy_id BIGINT NOT NULL COMMENT '策略 ID',
  version_no BIGINT NOT NULL COMMENT '策略版本号',
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '版本状态：DRAFT / PUBLISHED',
  config_json TEXT NOT NULL COMMENT '规范化策略配置 JSON',
  config_hash VARCHAR(64) NOT NULL COMMENT '策略配置哈希',
  created_by VARCHAR(64) NOT NULL COMMENT '创建用户 ID',
  created_at BIGINT NOT NULL COMMENT '创建时间戳（毫秒）',
  published_at BIGINT DEFAULT NULL COMMENT '发布时间戳（毫秒）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dg_ai_strategy_version_no (strategy_id, version_no),
  KEY idx_dg_ai_strategy_version_status (strategy_id, status),
  KEY idx_dg_ai_strategy_version_team (team_id, strategy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 优化策略版本表';

CREATE TABLE dg_operation_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  team_id VARCHAR(64) NOT NULL,
  document_id BIGINT DEFAULT NULL,
  actor_user_id VARCHAR(64) NOT NULL,
  action VARCHAR(64) NOT NULL,
  metadata_json VARCHAR(1000) NOT NULL DEFAULT '{}',
  created_at BIGINT NOT NULL,
  PRIMARY KEY (id),
  KEY idx_dg_log_team_created (team_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
