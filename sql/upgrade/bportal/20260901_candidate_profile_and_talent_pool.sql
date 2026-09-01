BEGIN;

CREATE TABLE IF NOT EXISTS candidate_profiles
(
    id                  BIGINT PRIMARY KEY,
    user_id             BIGINT       NOT NULL,
    real_name           VARCHAR(64),
    birth_year          SMALLINT,
    gender              VARCHAR(16),
    expected_salary_min NUMERIC(12, 2),
    expected_city       VARCHAR(64),
    education           VARCHAR(128),
    summary             VARCHAR(1000),
    profile_status      VARCHAR(16)  NOT NULL DEFAULT 'INCOMPLETE',
    last_active_at      TIMESTAMPTZ,
    version             INTEGER      NOT NULL DEFAULT 0,
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_by          BIGINT,
    trace_id            VARCHAR(128),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_candidate_profiles_status
        CHECK (profile_status IN ('INCOMPLETE', 'COMPLETE'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_candidate_profiles_user
    ON candidate_profiles (user_id) WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS enterprise_candidates
(
    id                  BIGINT PRIMARY KEY,
    enterprise_id       BIGINT       NOT NULL,
    linked_user_id      BIGINT,
    candidate_name      VARCHAR(64),
    phone_ciphertext    VARCHAR(512),
    email_ciphertext    VARCHAR(512),
    source              VARCHAR(32)  NOT NULL,
    profile_json        JSONB,
    status              VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_by          BIGINT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trace_id            VARCHAR(128),
    CONSTRAINT ck_enterprise_candidates_source
        CHECK (source IN ('CANDIDATE_APPLY', 'RESUME_IMPORT', 'MANUAL')),
    CONSTRAINT ck_enterprise_candidates_status
        CHECK (status IN ('ACTIVE', 'LINKED', 'ARCHIVED'))
);

CREATE INDEX IF NOT EXISTS idx_enterprise_candidates_status
    ON enterprise_candidates (enterprise_id, status, updated_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uq_enterprise_candidates_linked_user
    ON enterprise_candidates (enterprise_id, linked_user_id)
    WHERE linked_user_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS resume_import_batches
(
    id                  BIGINT PRIMARY KEY,
    enterprise_id       BIGINT       NOT NULL,
    operator_user_id    BIGINT       NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    total_count         INTEGER      NOT NULL,
    success_count       INTEGER      NOT NULL DEFAULT 0,
    failed_count        INTEGER      NOT NULL DEFAULT 0,
    completed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trace_id            VARCHAR(128),
    CONSTRAINT ck_resume_import_batches_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'PARTIAL_FAILED', 'FAILED')),
    CONSTRAINT ck_resume_import_batches_counts
        CHECK (total_count >= 0 AND success_count >= 0 AND failed_count >= 0
            AND success_count + failed_count <= total_count)
);

CREATE INDEX IF NOT EXISTS idx_resume_import_batches_enterprise
    ON resume_import_batches (enterprise_id, created_at DESC);

CREATE TABLE IF NOT EXISTS resume_import_items
(
    id                      BIGINT PRIMARY KEY,
    batch_id                BIGINT       NOT NULL,
    enterprise_id           BIGINT       NOT NULL,
    enterprise_candidate_id BIGINT,
    original_filename       VARCHAR(255) NOT NULL,
    object_key              VARCHAR(512) NOT NULL,
    file_hash               VARCHAR(128) NOT NULL,
    status                  VARCHAR(16)  NOT NULL,
    error_message           VARCHAR(512),
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trace_id                VARCHAR(128),
    CONSTRAINT ck_resume_import_items_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_resume_import_items_batch_status
    ON resume_import_items (batch_id, status, created_at);
CREATE UNIQUE INDEX IF NOT EXISTS uq_resume_import_items_success_hash
    ON resume_import_items (enterprise_id, file_hash)
    WHERE status = 'COMPLETED';

COMMIT;
