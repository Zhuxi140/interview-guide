-- 第六阶段：在线代码沙箱表结构初始化
-- PostgreSQL；跨模块关联仅使用逻辑外键，不创建物理 FOREIGN KEY。
-- 会话身份归 interview_sessions（Phase 3）所有，本阶段仅保存题目、用例与提交结果。

BEGIN;

-- 编程题库表
CREATE TABLE IF NOT EXISTS code_questions
(
    id                     BIGINT       PRIMARY KEY,
    visibility             VARCHAR(32)  NOT NULL DEFAULT 'PRIVATE',
    enterprise_id          BIGINT,
    title                  VARCHAR(128) NOT NULL,
    description            TEXT         NOT NULL,
    time_limit_ms          INT          NOT NULL DEFAULT 1000,
    memory_limit_mb        INT          NOT NULL DEFAULT 256,
    supported_languages    JSONB,
    version                INT          NOT NULL DEFAULT 0,
    is_deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by             BIGINT,
    trace_id               VARCHAR(128),
    CONSTRAINT chk_code_questions_visibility
        CHECK (visibility IN ('GLOBAL', 'PRIVATE')),
    CONSTRAINT chk_code_questions_tenant
        CHECK (
            (visibility = 'GLOBAL' AND enterprise_id IS NULL)
            OR (visibility = 'PRIVATE' AND enterprise_id IS NOT NULL)
        ),
    CONSTRAINT chk_code_questions_time_limit
        CHECK (time_limit_ms BETWEEN 100 AND 60000),
    CONSTRAINT chk_code_questions_memory_limit
        CHECK (memory_limit_mb BETWEEN 16 AND 1024),
    CONSTRAINT chk_code_questions_version
        CHECK (version >= 0),
    CONSTRAINT chk_code_questions_languages_json
        CHECK (
            supported_languages IS NULL
            OR JSONB_TYPEOF(supported_languages) = 'array'
        )
);

COMMENT ON TABLE code_questions IS '算法与编程题库；GLOBAL 为平台公共题，PRIVATE 为企业私有题';
COMMENT ON COLUMN code_questions.id IS '雪花主键';
COMMENT ON COLUMN code_questions.visibility IS 'GLOBAL (公共题库) / PRIVATE (企业私有)';
COMMENT ON COLUMN code_questions.enterprise_id IS '逻辑关联 enterprises.id，GLOBAL 时为 NULL';
COMMENT ON COLUMN code_questions.supported_languages IS '允许提交的编程语言列表（JSONB 数组）';
COMMENT ON COLUMN code_questions.version IS '乐观锁版本号，管理端编辑防覆盖';

CREATE INDEX IF NOT EXISTS idx_code_questions_enterprise
    ON code_questions (enterprise_id, id DESC)
    WHERE is_deleted = FALSE AND visibility = 'PRIVATE';

CREATE INDEX IF NOT EXISTS idx_code_questions_global
    ON code_questions (id DESC)
    WHERE is_deleted = FALSE AND visibility = 'GLOBAL';

-- 算法题目关联评测测试用例表
CREATE TABLE IF NOT EXISTS code_test_cases
(
    id              BIGINT      PRIMARY KEY,
    question_id     BIGINT      NOT NULL,
    input_case      TEXT        NOT NULL,
    expected_output TEXT        NOT NULL,
    is_secret       BOOLEAN     NOT NULL DEFAULT FALSE,
    version         INT         NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT,
    trace_id        VARCHAR(128),
    CONSTRAINT chk_code_test_cases_version
        CHECK (version >= 0)
);

COMMENT ON TABLE code_test_cases IS '算法题目关联评测测试用例；隐藏用例不向候选人下发';
COMMENT ON COLUMN code_test_cases.id IS '雪花主键';
COMMENT ON COLUMN code_test_cases.question_id IS '逻辑关联 code_questions.id';
COMMENT ON COLUMN code_test_cases.is_secret IS '是否为隐藏黑盒边界用例';
COMMENT ON COLUMN code_test_cases.version IS '乐观锁版本号，管理端编辑防覆盖';

CREATE INDEX IF NOT EXISTS idx_code_test_cases_question
    ON code_test_cases (question_id, id)
    WHERE is_deleted = FALSE;

-- 代码提交与运行记录表
CREATE TABLE IF NOT EXISTS code_submissions
(
    id               BIGINT       PRIMARY KEY,
    session_id       BIGINT       NOT NULL,
    enterprise_id    BIGINT       NOT NULL,
    question_id      BIGINT       NOT NULL,
    language         VARCHAR(32)  NOT NULL,
    submitted_code   TEXT         NOT NULL,
    execution_status VARCHAR(32),
    execution_time_ms INT,
    memory_used_mb   INT,
    ai_review_json   JSONB,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    trace_id         VARCHAR(128),
    CONSTRAINT chk_code_submissions_status
        CHECK (
            execution_status IS NULL
            OR execution_status IN ('QUEUED', 'RUNNING', 'PASS', 'FAIL', 'TIMEOUT', 'ERROR')
        ),
    CONSTRAINT chk_code_submissions_time
        CHECK (execution_time_ms IS NULL OR execution_time_ms >= 0),
    CONSTRAINT chk_code_submissions_memory
        CHECK (memory_used_mb IS NULL OR memory_used_mb >= 0),
    CONSTRAINT chk_code_submissions_review_json
        CHECK (
            ai_review_json IS NULL
            OR JSONB_TYPEOF(ai_review_json) = 'object'
        )
);

COMMENT ON TABLE code_submissions IS '代码提交与沙箱运行记录；提交为异步评测，状态由执行器回填';
COMMENT ON COLUMN code_submissions.id IS '雪花主键';
COMMENT ON COLUMN code_submissions.session_id IS '逻辑关联 interview_sessions.id';
COMMENT ON COLUMN code_submissions.enterprise_id IS '逻辑关联 enterprises.id，租户隔离键';
COMMENT ON COLUMN code_submissions.question_id IS '逻辑关联 code_questions.id';
COMMENT ON COLUMN code_submissions.execution_status IS 'QUEUED/RUNNING/PASS/FAIL/TIMEOUT/ERROR';
COMMENT ON COLUMN code_submissions.ai_review_json IS 'AI 静态审查与重构建议（JSON 对象）';

CREATE INDEX IF NOT EXISTS idx_code_submissions_session
    ON code_submissions (session_id, id DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_code_submissions_enterprise_created
    ON code_submissions (enterprise_id, created_at DESC, id DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_code_submissions_session_question
    ON code_submissions (session_id, question_id, id)
    WHERE is_deleted = FALSE;

-- 逐测试用例执行结果表
CREATE TABLE IF NOT EXISTS code_submission_results
(
    id               BIGINT      PRIMARY KEY,
    submission_id    BIGINT      NOT NULL,
    test_case_id     BIGINT      NOT NULL,
    passed           BOOLEAN     NOT NULL,
    actual_output    TEXT,
    execution_time_ms INT,
    memory_used_mb   INT,
    trace_id         VARCHAR(128),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted       BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_code_submission_results_time
        CHECK (execution_time_ms IS NULL OR execution_time_ms >= 0),
    CONSTRAINT chk_code_submission_results_memory
        CHECK (memory_used_mb IS NULL OR memory_used_mb >= 0)
);

COMMENT ON TABLE code_submission_results IS '逐测试用例执行结果；由沙箱执行器写入';
COMMENT ON COLUMN code_submission_results.id IS '雪花主键';
COMMENT ON COLUMN code_submission_results.submission_id IS '逻辑关联 code_submissions.id';
COMMENT ON COLUMN code_submission_results.test_case_id IS '逻辑关联 code_test_cases.id';

CREATE INDEX IF NOT EXISTS idx_code_submission_results_submission
    ON code_submission_results (submission_id, test_case_id);

COMMIT;
