ALTER TABLE dg_document
  ADD COLUMN has_unpublished_changes TINYINT(1) NOT NULL DEFAULT 0 COMMENT '已发布文档是否存在未发布的草稿改动：0-否，1-是' AFTER current_version_id;
