-- 简历上传截止时间和跨模块清理消息逻辑引用。
ALTER TABLE resumes
    ADD COLUMN IF NOT EXISTS upload_deadline_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS cleanup_message_id BIGINT;

-- 为升级前遗留的 UPLOADING 记录补充明确截止时间。
UPDATE resumes
SET upload_deadline_at = created_at + INTERVAL '15 minutes'
WHERE analyze_status = 'UPLOADING'
  AND upload_deadline_at IS NULL;

-- 逻辑删除后允许重新上传相同文件，但仍禁止同一用户存在两条有效重复简历。
DROP INDEX IF EXISTS idx_resumes_file_hash;
CREATE UNIQUE INDEX IF NOT EXISTS idx_resumes_file_hash
    ON resumes (user_id, file_hash)
    WHERE is_deleted = FALSE;

-- 仅加速仍占用配额的上传死记录扫描，不建立跨模块外键。
CREATE INDEX IF NOT EXISTS idx_resumes_upload_timeout
    ON resumes (upload_deadline_at, id)
    WHERE analyze_status = 'UPLOADING' AND is_deleted = FALSE;

-- 回滚提示：确认没有 UPLOADING 记录依赖截止时间后，删除索引和新增字段。
