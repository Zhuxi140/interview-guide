BEGIN;

ALTER TABLE interview_plan_drafts
    ADD COLUMN IF NOT EXISTS attempt_count INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS generation_deadline_at TIMESTAMPTZ;

ALTER TABLE interview_plan_drafts
    DROP CONSTRAINT IF EXISTS ck_interview_plan_generation_attempt;
ALTER TABLE interview_plan_drafts
    ADD CONSTRAINT ck_interview_plan_generation_attempt
    CHECK (attempt_count >= 0);

CREATE INDEX IF NOT EXISTS idx_interview_plan_generation_timeout
    ON interview_plan_drafts (generation_deadline_at, id)
    WHERE status = 'PROCESSING';

COMMENT ON COLUMN interview_plan_drafts.attempt_count
    IS 'Agent 生成已领取次数，消息租约重试栅栏';
COMMENT ON COLUMN interview_plan_drafts.generation_deadline_at
    IS 'Agent 生成执行截止时间，超时后允许其他消费端重新领取';

COMMIT;