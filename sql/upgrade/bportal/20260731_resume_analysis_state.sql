BEGIN;

ALTER TABLE resumes
    ADD COLUMN IF NOT EXISTS analysis_message_id BIGINT,
    ADD COLUMN IF NOT EXISTS analysis_idempotency_key_hash VARCHAR(64),
    ADD COLUMN IF NOT EXISTS analysis_attempt_count INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS analysis_deadline_at TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS uk_resumes_analysis_message
    ON resumes (analysis_message_id)
    WHERE analysis_message_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_resumes_analysis_timeout
    ON resumes (analysis_deadline_at, id)
    WHERE analyze_status = 'PROCESSING' AND is_deleted = FALSE;

COMMENT ON COLUMN resumes.analysis_message_id
    IS '当前简历 AI 分析消息 ID，同时作为对外 taskId，不建立跨模块外键';
COMMENT ON COLUMN resumes.analysis_idempotency_key_hash
    IS '当前简历 AI 分析请求幂等键 SHA-256 摘要';
COMMENT ON COLUMN resumes.analysis_attempt_count
    IS '当前简历 AI 分析已领取次数';
COMMENT ON COLUMN resumes.analysis_deadline_at
    IS '当前简历 AI 分析执行截止时间';
COMMENT ON COLUMN resume_analyses.id
    IS '分析消息 ID，同时作为对外 taskId';

COMMIT;
