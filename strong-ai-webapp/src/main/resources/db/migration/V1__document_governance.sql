CREATE TABLE dg_document (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  team_id VARCHAR(64) NOT NULL COMMENT '团队 ID',
  owner_user_id VARCHAR(64) NOT NULL COMMENT '文档所属用户 ID',
  title VARCHAR(200) NOT NULL COMMENT '文档标题',
  document_type VARCHAR(64) NOT NULL COMMENT '文档类型',
  tags VARCHAR(500) NOT NULL DEFAULT '' COMMENT '标签，多个标签以分隔符连接',
  status VARCHAR(16) NOT NULL COMMENT '文档状态',
  draft_content MEDIUMTEXT NOT NULL COMMENT '草稿内容',
  current_version_id BIGINT DEFAULT NULL COMMENT '当前发布版本 ID',
  created_at BIGINT NOT NULL COMMENT '创建时间戳（毫秒）',
  updated_at BIGINT NOT NULL COMMENT '更新时间戳（毫秒）',
  PRIMARY KEY (id),
  KEY idx_dg_document_team_updated (team_id, updated_at),
  KEY idx_dg_document_team_owner (team_id, owner_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档表';

CREATE TABLE dg_document_version (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  document_id BIGINT NOT NULL COMMENT '文档 ID',
  team_id VARCHAR(64) NOT NULL COMMENT '团队 ID',
  version_no BIGINT NOT NULL COMMENT '版本号',
  content MEDIUMTEXT NOT NULL COMMENT '版本内容',
  diff_content MEDIUMTEXT NOT NULL COMMENT '与上一版本的差异内容',
  release_note VARCHAR(500) NOT NULL DEFAULT '' COMMENT '发布说明',
  created_by VARCHAR(64) NOT NULL COMMENT '创建用户 ID',
  created_at BIGINT NOT NULL COMMENT '创建时间戳（毫秒）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dg_version_document_no (document_id, version_no),
  KEY idx_dg_version_team_document (team_id, document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档版本表';

CREATE TABLE dg_template (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  team_id VARCHAR(64) NOT NULL COMMENT '团队 ID',
  name VARCHAR(100) NOT NULL COMMENT '模板名称',
  document_type VARCHAR(64) NOT NULL COMMENT '适用文档类型',
  required_headings TEXT NOT NULL COMMENT '必需标题配置',
  created_by VARCHAR(64) NOT NULL COMMENT '创建用户 ID',
  created_at BIGINT NOT NULL COMMENT '创建时间戳（毫秒）',
  updated_at BIGINT NOT NULL COMMENT '更新时间戳（毫秒）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dg_template_team_name (team_id, name),
  KEY idx_dg_template_team_type (team_id, document_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档模板表';

CREATE TABLE dg_term (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  team_id VARCHAR(64) NOT NULL COMMENT '团队 ID',
  term VARCHAR(200) NOT NULL COMMENT '术语',
  preferred_term VARCHAR(200) NOT NULL DEFAULT '' COMMENT '推荐术语',
  is_forbidden TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否禁用：0-否，1-是',
  description VARCHAR(500) NOT NULL DEFAULT '' COMMENT '术语说明',
  created_by VARCHAR(64) NOT NULL COMMENT '创建用户 ID',
  created_at BIGINT NOT NULL COMMENT '创建时间戳（毫秒）',
  updated_at BIGINT NOT NULL COMMENT '更新时间戳（毫秒）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dg_term_team_term (team_id, term),
  KEY idx_dg_term_team (team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='术语表';

CREATE TABLE dg_rule_set (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  team_id VARCHAR(64) NOT NULL COMMENT '团队 ID',
  name VARCHAR(100) NOT NULL COMMENT '规则集名称',
  config_json TEXT NOT NULL COMMENT '规则配置 JSON',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用：0-否，1-是',
  created_by VARCHAR(64) NOT NULL COMMENT '创建用户 ID',
  created_at BIGINT NOT NULL COMMENT '创建时间戳（毫秒）',
  updated_at BIGINT NOT NULL COMMENT '更新时间戳（毫秒）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dg_rule_set_team_name (team_id, name),
  KEY idx_dg_rule_set_team (team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则集表';

CREATE TABLE dg_quality_report (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  team_id VARCHAR(64) NOT NULL COMMENT '团队 ID',
  document_id BIGINT NOT NULL COMMENT '文档 ID',
  document_version_id BIGINT DEFAULT NULL COMMENT '文档版本 ID',
  score INT NOT NULL COMMENT '质量评分',
  checked_by VARCHAR(64) NOT NULL COMMENT '检查用户 ID',
  created_at BIGINT NOT NULL COMMENT '创建时间戳（毫秒）',
  PRIMARY KEY (id),
  KEY idx_dg_report_team_document (team_id, document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质量报告表';

CREATE TABLE dg_quality_issue (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  team_id VARCHAR(64) NOT NULL COMMENT '团队 ID',
  report_id BIGINT NOT NULL COMMENT '质量报告 ID',
  rule_code VARCHAR(64) NOT NULL COMMENT '规则编码',
  severity VARCHAR(16) NOT NULL COMMENT '问题严重级别',
  line_number INT NOT NULL COMMENT '问题所在行号',
  message VARCHAR(500) NOT NULL COMMENT '问题描述',
  suggestion VARCHAR(500) NOT NULL COMMENT '修改建议',
  PRIMARY KEY (id),
  KEY idx_dg_issue_team_report (team_id, report_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质量问题表';

CREATE TABLE dg_ai_task (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  team_id VARCHAR(64) NOT NULL COMMENT '团队 ID',
  document_id BIGINT NOT NULL COMMENT '文档 ID',
  idempotency_key VARCHAR(100) NOT NULL COMMENT '幂等键',
  created_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '任务创建用户 ID',
  status VARCHAR(16) NOT NULL COMMENT '任务状态',
  provider VARCHAR(64) NOT NULL COMMENT 'AI 服务提供商',
  model VARCHAR(100) NOT NULL COMMENT 'AI 模型名称',
  result_summary VARCHAR(500) NOT NULL DEFAULT '' COMMENT '任务结果摘要',
  error_code VARCHAR(64) NOT NULL DEFAULT '' COMMENT '错误码',
  prompt_tokens BIGINT DEFAULT NULL COMMENT '提示词 Token 数',
  completion_tokens BIGINT DEFAULT NULL COMMENT '生成内容 Token 数',
  total_tokens BIGINT DEFAULT NULL COMMENT '总 Token 数',
  created_at BIGINT NOT NULL COMMENT '创建时间戳（毫秒）',
  updated_at BIGINT NOT NULL COMMENT '更新时间戳（毫秒）',
  expires_at BIGINT NOT NULL COMMENT '过期时间戳（毫秒）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dg_task_idempotency (team_id, document_id, idempotency_key),
  KEY idx_dg_task_team_expiry (team_id, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 任务表';

CREATE TABLE dg_operation_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  team_id VARCHAR(64) NOT NULL COMMENT '团队 ID',
  document_id BIGINT DEFAULT NULL COMMENT '文档 ID',
  actor_user_id VARCHAR(64) NOT NULL COMMENT '操作用户 ID',
  action VARCHAR(64) NOT NULL COMMENT '操作类型',
  metadata_json VARCHAR(1000) NOT NULL DEFAULT '{}' COMMENT '操作元数据 JSON',
  created_at BIGINT NOT NULL COMMENT '创建时间戳（毫秒）',
  PRIMARY KEY (id),
  KEY idx_dg_log_team_created (team_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';
