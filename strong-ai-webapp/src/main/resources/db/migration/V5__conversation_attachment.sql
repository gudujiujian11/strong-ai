CREATE TABLE ai_conversation_attachment (
  id VARCHAR(64) NOT NULL COMMENT 'Attachment ID',
  conversation_id VARCHAR(64) NOT NULL COMMENT 'Conversation ID',
  message_id VARCHAR(64) DEFAULT NULL COMMENT 'Bound user message ID',
  original_name VARCHAR(255) NOT NULL COMMENT 'Original file name',
  content_type VARCHAR(64) NOT NULL COMMENT 'Validated image MIME type',
  size_bytes BIGINT NOT NULL COMMENT 'Image size in bytes',
  content LONGBLOB NOT NULL COMMENT 'Image binary content',
  created_at BIGINT NOT NULL COMMENT 'Upload timestamp in milliseconds',
  expires_at BIGINT DEFAULT NULL COMMENT 'Unbound attachment expiration timestamp',
  PRIMARY KEY (id),
  KEY idx_ai_conversation_attachment_conversation (conversation_id),
  KEY idx_ai_conversation_attachment_message (message_id),
  KEY idx_ai_conversation_attachment_expiration (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI conversation image attachments';
