ALTER TABLE dg_ai_task
  ADD COLUMN result_summary VARCHAR(500) NOT NULL DEFAULT '' AFTER model,
  ADD COLUMN created_by VARCHAR(64) NOT NULL DEFAULT '' AFTER idempotency_key;
