CREATE TABLE ai_image_parse_history (
  id VARCHAR(64) NOT NULL COMMENT 'Parse job ID',
  original_name VARCHAR(255) NOT NULL COMMENT 'Original image file name',
  content_type VARCHAR(64) NOT NULL COMMENT 'Image MIME type',
  size_bytes BIGINT NOT NULL COMMENT 'Image size in bytes',
  content LONGBLOB NOT NULL COMMENT 'Original image binary content',
  result_json MEDIUMTEXT NOT NULL COMMENT 'Serialized Markdown parse result',
  provider VARCHAR(64) DEFAULT NULL COMMENT 'Model provider',
  model VARCHAR(128) DEFAULT NULL COMMENT 'Model name',
  created_at BIGINT NOT NULL COMMENT 'Parse completion timestamp in milliseconds',
  PRIMARY KEY (id),
  KEY idx_ai_image_parse_history_created (created_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Shared image Markdown parse history';
