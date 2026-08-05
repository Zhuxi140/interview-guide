BEGIN;

ALTER TABLE interview_plan_drafts
    ADD COLUMN IF NOT EXISTS apply_idempotency_key VARCHAR(128);
ALTER TABLE interview_plan_drafts
    ADD COLUMN IF NOT EXISTS applied_plan_json JSONB;
ALTER TABLE interview_plan_drafts
    ADD COLUMN IF NOT EXISTS applied_schedule_ids JSONB;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'interview_plan_drafts'::regclass
          AND conname = 'ck_interview_plan_applied_object'
    ) THEN
        ALTER TABLE interview_plan_drafts
            ADD CONSTRAINT ck_interview_plan_applied_object
            CHECK (applied_plan_json IS NULL OR jsonb_typeof(applied_plan_json) = 'object');
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'interview_plan_drafts'::regclass
          AND conname = 'ck_interview_plan_applied_ids'
    ) THEN
        ALTER TABLE interview_plan_drafts
            ADD CONSTRAINT ck_interview_plan_applied_ids
            CHECK (applied_schedule_ids IS NULL OR jsonb_typeof(applied_schedule_ids) = 'array');
    END IF;
END
$$;

COMMENT ON COLUMN interview_plan_drafts.apply_idempotency_key IS 'HR 应用草案时的幂等键，仅 APPLIED 后有值';
COMMENT ON COLUMN interview_plan_drafts.applied_plan_json IS 'HR 应用时提交的修订后计划快照，用于幂等对比与页面刷新恢复';
COMMENT ON COLUMN interview_plan_drafts.applied_schedule_ids IS '应用时创建的排期 ID 数组快照，用于幂等重放';

COMMIT;
