-- 第五阶段：语音面试表结构初始化
-- PostgreSQL；跨模块关联仅使用逻辑外键，不创建物理 FOREIGN KEY。
-- 会话生命周期以 interview_sessions 为唯一事实来源。

BEGIN;

-- 语音面试扩展表
CREATE TABLE IF NOT EXISTS voice_interview_sessions
(
    id                      BIGINT       PRIMARY KEY,
    interview_session_id    BIGINT       NOT NULL,
    skill_code              VARCHAR(64),
    current_phase           VARCHAR(32)  NOT NULL DEFAULT 'INTRO',
    actual_duration_seconds INT          NOT NULL DEFAULT 0,
    recording_object_key    VARCHAR(512),
    recording_consent_at    TIMESTAMPTZ,
    retention_until         TIMESTAMPTZ,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trace_id                VARCHAR(128),
    CONSTRAINT uq_voice_interview_sessions_session
        UNIQUE (interview_session_id),
    CONSTRAINT chk_voice_interview_sessions_phase
        CHECK (current_phase IN ('INTRO', 'TECH', 'PROJECT', 'HR')),
    CONSTRAINT chk_voice_interview_sessions_duration
        CHECK (actual_duration_seconds >= 0),
    CONSTRAINT chk_voice_interview_sessions_recording
        CHECK (
            recording_object_key IS NULL
            OR (recording_consent_at IS NOT NULL AND retention_until IS NOT NULL)
        )
);

COMMENT ON TABLE voice_interview_sessions IS '语音面试扩展表；会话身份和生命周期由 interview_sessions 管理';
COMMENT ON COLUMN voice_interview_sessions.id IS '雪花主键';
COMMENT ON COLUMN voice_interview_sessions.interview_session_id IS '逻辑关联 interview_sessions.id';
COMMENT ON COLUMN voice_interview_sessions.recording_object_key IS '获得明确授权后保存的私有录音对象键';
COMMENT ON COLUMN voice_interview_sessions.retention_until IS '录音及关联转写的保留截止时间';

-- 语音消息明细表
CREATE TABLE IF NOT EXISTS voice_interview_messages
(
    id                   BIGINT       PRIMARY KEY,
    interview_session_id BIGINT       NOT NULL,
    enterprise_id        BIGINT       NOT NULL,
    event_id             VARCHAR(64)  NOT NULL,
    message_type         VARCHAR(20)  NOT NULL,
    current_phase        VARCHAR(32),
    parent_message_id    BIGINT,
    follow_up_depth      SMALLINT     NOT NULL DEFAULT 0,
    asr_text             TEXT,
    llm_response_text    TEXT,
    sequence_num         BIGINT       NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trace_id             VARCHAR(128) NOT NULL,
    is_deleted           BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_voice_interview_messages_type
        CHECK (message_type IN ('USER_SPEECH', 'AI_SPEECH', 'SYSTEM')),
    CONSTRAINT chk_voice_interview_messages_depth
        CHECK (follow_up_depth >= 0),
    CONSTRAINT chk_voice_interview_messages_content
        CHECK (
            (message_type = 'USER_SPEECH' AND NULLIF(BTRIM(asr_text), '') IS NOT NULL)
            OR (message_type = 'AI_SPEECH' AND NULLIF(BTRIM(llm_response_text), '') IS NOT NULL)
            OR message_type = 'SYSTEM'
        )
);

COMMENT ON TABLE voice_interview_messages IS '语音面试最终 ASR 与完整 AI 回复明细；事件顺序以 interview_timeline_events 为准';
COMMENT ON COLUMN voice_interview_messages.id IS '雪花主键';
COMMENT ON COLUMN voice_interview_messages.interview_session_id IS '逻辑关联 interview_sessions.id';
COMMENT ON COLUMN voice_interview_messages.enterprise_id IS '逻辑关联 enterprises.id，租户隔离键';
COMMENT ON COLUMN voice_interview_messages.event_id IS '与 interview_timeline_events 共享的事件幂等 ID';
COMMENT ON COLUMN voice_interview_messages.parent_message_id IS '逻辑关联 voice_interview_messages.id';
COMMENT ON COLUMN voice_interview_messages.sequence_num IS '从 interview_sessions.last_event_sequence 原子分配';

CREATE UNIQUE INDEX IF NOT EXISTS uq_voice_messages_session_sequence
    ON voice_interview_messages (interview_session_id, sequence_num);

CREATE UNIQUE INDEX IF NOT EXISTS uq_voice_messages_session_event
    ON voice_interview_messages (interview_session_id, event_id);

CREATE INDEX IF NOT EXISTS idx_voice_messages_enterprise_created
    ON voice_interview_messages (enterprise_id, created_at DESC, id DESC);

-- 语音面试异步评估任务与结果表
CREATE TABLE IF NOT EXISTS voice_interview_evaluations
(
    id                          BIGINT       PRIMARY KEY,
    interview_session_id        BIGINT       NOT NULL,
    enterprise_id               BIGINT       NOT NULL,
    evaluation_status           VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    attempt_no                  INT          NOT NULL DEFAULT 0,
    failure_reason              VARCHAR(512),
    overall_score               INT,
    question_evaluations_json   JSONB,
    strengths_json              JSONB,
    improvements_json           JSONB,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at                  TIMESTAMPTZ,
    completed_at                TIMESTAMPTZ,
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted                  BOOLEAN      NOT NULL DEFAULT FALSE,
    trace_id                    VARCHAR(128) NOT NULL,
    CONSTRAINT uq_voice_evaluations_session
        UNIQUE (interview_session_id),
    CONSTRAINT chk_voice_evaluations_status
        CHECK (evaluation_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_voice_evaluations_attempt
        CHECK (attempt_no >= 0),
    CONSTRAINT chk_voice_evaluations_score
        CHECK (overall_score IS NULL OR overall_score BETWEEN 0 AND 100),
    CONSTRAINT chk_voice_evaluations_completed
        CHECK (
            evaluation_status <> 'COMPLETED'
            OR (overall_score IS NOT NULL AND completed_at IS NOT NULL)
        ),
    CONSTRAINT chk_voice_evaluations_failed
        CHECK (
            evaluation_status <> 'FAILED'
            OR (NULLIF(BTRIM(failure_reason), '') IS NOT NULL AND completed_at IS NOT NULL)
        ),
    CONSTRAINT chk_voice_evaluations_questions_json
        CHECK (
            question_evaluations_json IS NULL
            OR JSONB_TYPEOF(question_evaluations_json) = 'array'
        ),
    CONSTRAINT chk_voice_evaluations_strengths_json
        CHECK (
            strengths_json IS NULL
            OR JSONB_TYPEOF(strengths_json) = 'array'
        ),
    CONSTRAINT chk_voice_evaluations_improvements_json
        CHECK (
            improvements_json IS NULL
            OR JSONB_TYPEOF(improvements_json) = 'array'
        )
);

COMMENT ON TABLE voice_interview_evaluations IS '语音面试异步评估任务状态与单场结果快照';
COMMENT ON COLUMN voice_interview_evaluations.id IS '雪花主键';
COMMENT ON COLUMN voice_interview_evaluations.interview_session_id IS '逻辑关联 interview_sessions.id；一场会话仅一份评估';
COMMENT ON COLUMN voice_interview_evaluations.enterprise_id IS '逻辑关联 enterprises.id，租户隔离键';
COMMENT ON COLUMN voice_interview_evaluations.attempt_no IS '已经开始的评估尝试次数';

CREATE INDEX IF NOT EXISTS idx_voice_evaluations_pending
    ON voice_interview_evaluations (evaluation_status, updated_at)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_voice_evaluations_enterprise_created
    ON voice_interview_evaluations (enterprise_id, created_at DESC, id DESC)
    WHERE is_deleted = FALSE;

COMMIT;
