BEGIN;

-- 旧画像表没有可安全自动映射的语义；存在数据时停止迁移，要求人工确认。
DO $$
DECLARE
    legacy_profile_has_rows BOOLEAN;
BEGIN
    IF to_regclass('public.candidate_profile') IS NOT NULL THEN
        EXECUTE 'SELECT EXISTS (SELECT 1 FROM candidate_profile LIMIT 1)'
            INTO legacy_profile_has_rows;
        IF legacy_profile_has_rows THEN
            RAISE EXCEPTION 'candidate_profile contains data; manual migration required';
        END IF;
    END IF;
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'candidate_skill_scores'
          AND column_name = 'resume_analysis_id'
    ) AND EXISTS (SELECT 1 FROM candidate_skill_scores LIMIT 1) THEN
        RAISE EXCEPTION 'candidate_skill_scores contains legacy data; manual migration required';
    END IF;
END $$;

DO $$
BEGIN
    IF to_regclass('public.candidate_profile') IS NOT NULL THEN
        EXECUTE 'DROP TABLE candidate_profile';
    END IF;
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'candidate_skill_scores'
          AND column_name = 'resume_analysis_id'
    ) THEN
        EXECUTE 'DROP TABLE candidate_skill_scores';
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS candidate_ai_profiles (
    id BIGINT NOT NULL PRIMARY KEY,
    candidate_id BIGINT NOT NULL,
    resume_id BIGINT NOT NULL,
    source_application_id BIGINT,
    source_enterprise_id BIGINT,
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    profile_schema_version VARCHAR(32) NOT NULL,
    summary_json JSONB CHECK (summary_json IS NULL OR jsonb_typeof(summary_json) = 'object'),
    llm_config_snapshot JSONB
        CHECK (llm_config_snapshot IS NULL OR jsonb_typeof(llm_config_snapshot) = 'object'),
    attempt_count INT NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    deadline_at TIMESTAMPTZ,
    failure_reason TEXT,
    analyzed_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    trace_id VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE candidate_ai_profiles IS '一份简历的一版岗位无关 AI 人才画像';
COMMENT ON COLUMN candidate_ai_profiles.id IS '画像消息 ID，同时作为 taskId';
COMMENT ON COLUMN candidate_ai_profiles.profile_schema_version IS '画像维度和评分规范版本';
CREATE INDEX IF NOT EXISTS idx_candidate_ai_profiles_candidate
    ON candidate_ai_profiles (candidate_id, created_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uk_candidate_ai_profiles_completed
    ON candidate_ai_profiles (resume_id, profile_schema_version)
    WHERE status = 'COMPLETED' AND is_deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uk_candidate_ai_profiles_active
    ON candidate_ai_profiles (resume_id, profile_schema_version)
    WHERE status IN ('PENDING', 'PROCESSING') AND is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS candidate_skill_scores (
    id BIGINT NOT NULL PRIMARY KEY,
    candidate_profile_id BIGINT NOT NULL,
    dimension_code VARCHAR(32) NOT NULL,
    score INT NOT NULL CHECK (score BETWEEN 0 AND 100),
    ai_justification TEXT,
    evidence_json JSONB NOT NULL DEFAULT '[]'
        CHECK (jsonb_typeof(evidence_json) = 'array'),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE candidate_skill_scores IS 'AI 人才画像固定维度评分明细';
CREATE UNIQUE INDEX IF NOT EXISTS idx_skill_scores_unique
    ON candidate_skill_scores (candidate_profile_id, dimension_code);

CREATE TABLE IF NOT EXISTS job_screening_configs (
    job_id BIGINT NOT NULL PRIMARY KEY,
    enterprise_id BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    overall_threshold INT NOT NULL CHECK (overall_threshold BETWEEN 0 AND 100),
    dimension_thresholds JSONB NOT NULL DEFAULT '{}'
        CHECK (jsonb_typeof(dimension_thresholds) = 'object'),
    version INT NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_by BIGINT,
    updated_by BIGINT,
    trace_id VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_job_screening_configs_enterprise
    ON job_screening_configs (enterprise_id, enabled);

CREATE TABLE IF NOT EXISTS application_ai_screenings (
    id BIGINT NOT NULL PRIMARY KEY,
    application_id BIGINT NOT NULL,
    candidate_profile_id BIGINT,
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('WAITING_PROFILE', 'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    overall_match_score INT CHECK (overall_match_score BETWEEN 0 AND 100),
    dimension_matches_json JSONB,
    recommendation VARCHAR(32)
        CHECK (recommendation IN ('RECOMMEND_PASS', 'RECOMMEND_REJECT')),
    threshold_snapshot JSONB NOT NULL,
    job_snapshot JSONB NOT NULL,
    llm_config_snapshot JSONB,
    review_decision VARCHAR(16) CHECK (review_decision IN ('PASSED', 'REJECTED')),
    reviewed_by BIGINT,
    reviewed_at TIMESTAMPTZ,
    attempt_count INT NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    deadline_at TIMESTAMPTZ,
    failure_reason TEXT,
    idempotency_key_hash VARCHAR(64) NOT NULL,
    trace_id VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_application_ai_screenings_application
    ON application_ai_screenings (application_id, created_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uk_application_ai_screenings_idempotency
    ON application_ai_screenings (application_id, idempotency_key_hash);
CREATE UNIQUE INDEX IF NOT EXISTS uk_application_ai_screenings_active
    ON application_ai_screenings (application_id)
    WHERE status IN ('WAITING_PROFILE', 'PENDING', 'PROCESSING');

CREATE TABLE IF NOT EXISTS candidate_job_match_analyses (
    id BIGINT NOT NULL PRIMARY KEY,
    application_id BIGINT NOT NULL,
    candidate_profile_id BIGINT,
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('WAITING_PROFILE', 'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    match_score INT CHECK (match_score BETWEEN 0 AND 100),
    pass_probability INT CHECK (pass_probability BETWEEN 0 AND 100),
    strengths_json JSONB,
    gaps_json JSONB,
    job_snapshot JSONB NOT NULL,
    llm_config_snapshot JSONB,
    attempt_count INT NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    deadline_at TIMESTAMPTZ,
    failure_reason TEXT,
    idempotency_key_hash VARCHAR(64) NOT NULL,
    analyzed_at TIMESTAMPTZ,
    trace_id VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_candidate_job_match_application
    ON candidate_job_match_analyses (application_id, created_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uk_candidate_job_match_idempotency
    ON candidate_job_match_analyses (application_id, idempotency_key_hash);
CREATE UNIQUE INDEX IF NOT EXISTS uk_candidate_job_match_active
    ON candidate_job_match_analyses (application_id)
    WHERE status IN ('WAITING_PROFILE', 'PENDING', 'PROCESSING');

ALTER TABLE job_applications DROP COLUMN IF EXISTS ai_match_score;

DELETE FROM llm_scene_config WHERE scene_code = 'JOB_RESUME_MATCHING';
INSERT INTO llm_scene_config (
    scene_code, model_type, provider_id, temperature, top_p,
    max_input_tokens, max_output_tokens, timeout_seconds,
    prompt_version, extra_options, enabled, version, created_at, updated_at
) VALUES
('CANDIDATE_PROFILE_GENERATION', 'CHAT', NULL, 0.200, 0.900,
 16000, 2000, 60, 'v1', '{}'::jsonb, TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('HR_APPLICATION_SCREENING', 'CHAT', NULL, 0.200, 0.900,
 16000, 1600, 60, 'v1', '{}'::jsonb, TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CANDIDATE_JOB_MATCHING', 'CHAT', NULL, 0.200, 0.900,
 16000, 1200, 60, 'v1', '{}'::jsonb, TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (scene_code) DO NOTHING;

COMMIT;
