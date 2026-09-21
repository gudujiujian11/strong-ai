CREATE TABLE ai_conversation (
  id VARCHAR(64) NOT NULL COMMENT 'Conversation ID',
  title VARCHAR(255) NOT NULL COMMENT 'Conversation title',
  provider VARCHAR(64) NOT NULL COMMENT 'Model provider',
  model VARCHAR(128) NOT NULL COMMENT 'Model name',
  deep_thinking TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Deep thinking enabled',
  created_at BIGINT NOT NULL COMMENT 'Creation timestamp in milliseconds',
  updated_at BIGINT NOT NULL COMMENT 'Update timestamp in milliseconds',
  PRIMARY KEY (id),
  KEY idx_ai_conversation_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI conversations';

CREATE TABLE ai_conversation_message (
  id VARCHAR(64) NOT NULL COMMENT 'Message ID',
  conversation_id VARCHAR(64) NOT NULL COMMENT 'Conversation ID',
  message_order INT NOT NULL COMMENT 'Message order within the conversation',
  role VARCHAR(32) NOT NULL COMMENT 'Message role',
  content MEDIUMTEXT NOT NULL COMMENT 'Message content',
  reasoning MEDIUMTEXT DEFAULT NULL COMMENT 'Reasoning content',
  provider VARCHAR(64) DEFAULT NULL COMMENT 'Model provider',
  model VARCHAR(128) DEFAULT NULL COMMENT 'Model name',
  prompt_tokens BIGINT DEFAULT NULL COMMENT 'Prompt token count',
  completion_tokens BIGINT DEFAULT NULL COMMENT 'Completion token count',
  total_tokens BIGINT DEFAULT NULL COMMENT 'Total token count',
  created_at BIGINT NOT NULL COMMENT 'Creation timestamp in milliseconds',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_conversation_message_order (conversation_id, message_order),
  KEY idx_ai_conversation_message_conversation (conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI conversation messages';
