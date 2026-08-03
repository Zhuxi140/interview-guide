BEGIN;

CREATE TABLE IF NOT EXISTS candidate_interview_availability (
    candidate_user_id BIGINT       NOT NULL PRIMARY KEY,
    timezone          VARCHAR(64)  NOT NULL,
    ranges_json       JSONB        NOT NULL DEFAULT '[]'::JSONB,
    version           INT          NOT NULL DEFAULT 0,
    updated_by        BIGINT,
    trace_id          VARCHAR(128),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_candidate_availability_ranges
        CHECK (jsonb_typeof(ranges_json) = 'array'),
    CONSTRAINT ck_candidate_availability_version
        CHECK (version >= 0)
);

COMMENT ON TABLE candidate_interview_availability IS '候选人可面试时间聚合配置表';
COMMENT ON COLUMN candidate_interview_availability.candidate_user_id IS '[逻辑外键]→sys_users，同时作为主键';
COMMENT ON COLUMN candidate_interview_availability.timezone IS 'IANA 时区';
COMMENT ON COLUMN candidate_interview_availability.ranges_json IS '按开始时间排序的可面试时间范围数组';
COMMENT ON COLUMN candidate_interview_availability.version IS '完整替换配置使用的乐观锁版本';

COMMIT;
