-- ============================================================
-- BPortal：禁止同一面试官的有效排期时间重叠
-- PostgreSQL 14+
-- ============================================================

BEGIN;

CREATE EXTENSION IF NOT EXISTS btree_gist;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'interview_schedule'::regclass
          AND conname = 'ex_interview_schedule_interviewer_time'
    ) THEN
        ALTER TABLE interview_schedule
            ADD CONSTRAINT ex_interview_schedule_interviewer_time
            EXCLUDE USING gist (
                interviewer_user_id WITH =,
                tsrange(
                    interview_time AT TIME ZONE 'UTC',
                    (interview_time AT TIME ZONE 'UTC')
                        + duration_minutes * INTERVAL '1 minute',
                    '[)'
                ) WITH &&
            )
            WHERE (
                is_deleted = FALSE
                AND interviewer_user_id IS NOT NULL
                AND status IN ('PENDING_CONFIRMATION', 'CONFIRMED', 'IN_PROGRESS')
            );
    END IF;
END
$$;

COMMIT;
