BEGIN;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'interview_schedule'
          AND column_name = 'stage_code'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'interview_schedule'
          AND column_name = 'phase_code'
    ) THEN
        ALTER TABLE interview_schedule RENAME COLUMN stage_code TO phase_code;
    END IF;
END
$$;

ALTER TABLE interview_schedule
    ADD COLUMN IF NOT EXISTS template_snapshot_json JSONB;
ALTER TABLE interview_schedule
    ALTER COLUMN phase_code SET NOT NULL,
    ALTER COLUMN template_snapshot_json SET NOT NULL;
ALTER TABLE interview_plan_drafts
    ALTER COLUMN template_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'interview_phase_configs'::regclass
          AND conname = 'ck_interview_phase_code'
    ) THEN
        ALTER TABLE interview_phase_configs
            ADD CONSTRAINT ck_interview_phase_code
            CHECK (phase_code ~ '^[A-Z][A-Z0-9_]{0,31}$');
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'interview_schedule'::regclass
          AND conname = 'ck_interview_schedule_phase_code'
    ) THEN
        ALTER TABLE interview_schedule
            ADD CONSTRAINT ck_interview_schedule_phase_code
            CHECK (phase_code ~ '^[A-Z][A-Z0-9_]{0,31}$');
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'interview_schedule'::regclass
          AND conname = 'ck_interview_schedule_template_snapshot'
    ) THEN
        ALTER TABLE interview_schedule
            ADD CONSTRAINT ck_interview_schedule_template_snapshot
            CHECK (jsonb_typeof(template_snapshot_json) = 'object');
    END IF;
END
$$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_schedule_application_phase_active
    ON interview_schedule (application_id, phase_code)
    WHERE is_deleted = FALSE;

COMMENT ON COLUMN interview_schedule.round_no IS '投递下的面试轮次，对应模板阶段 sortOrder';
COMMENT ON COLUMN interview_schedule.phase_code IS '由模板快照按 round_no 派生的阶段编码';
COMMENT ON COLUMN interview_schedule.template_snapshot_json IS '首轮排期固化的完整模板及阶段组卷配置快照';

COMMIT;
