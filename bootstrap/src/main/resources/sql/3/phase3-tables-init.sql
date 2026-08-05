-- ============================================================
-- Phase 3: 文本面试核心链路建表脚本（PostgreSQL）
-- ============================================================
-- 约束：
-- 1. 业务主键由应用侧雪花算法生成，不使用数据库自增。
-- 2. 不创建物理外键，跨表引用由所属模块在应用层校验。
-- 3. 招聘结果归 job_applications，interview_schedule 只描述单轮面试。

BEGIN;

CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ==================== 1. interview_stage_templates ====================
CREATE TABLE IF NOT EXISTS interview_stage_templates (
    id                      BIGINT          NOT NULL,
    enterprise_id           BIGINT          NOT NULL,
    template_name           VARCHAR(64)     NOT NULL,
    stages_sequence_json    JSONB           NOT NULL,
    version                 INT             NOT NULL DEFAULT 0,
    is_deleted              BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by              BIGINT,
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_interview_template_stages_array
        CHECK (jsonb_typeof(stages_sequence_json) = 'array'),
    CONSTRAINT ck_interview_template_version
        CHECK (version >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_template_enterprise_name
    ON interview_stage_templates (enterprise_id, template_name)
    WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_interview_template_enterprise_updated
    ON interview_stage_templates (enterprise_id, updated_at DESC, id DESC);

COMMENT ON TABLE interview_stage_templates IS '企业自定义面试阶段模板表';
COMMENT ON COLUMN interview_stage_templates.id IS '雪花主键';
COMMENT ON COLUMN interview_stage_templates.enterprise_id IS '[逻辑外键]→enterprises';
COMMENT ON COLUMN interview_stage_templates.stages_sequence_json IS '阶段节点编排 JSON 数组';
COMMENT ON COLUMN interview_stage_templates.version IS '管理端编辑使用的乐观锁版本号';
COMMENT ON COLUMN interview_stage_templates.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN interview_stage_templates.updated_by IS '[逻辑外键]→sys_users';

-- ==================== 2. interview_phase_configs ====================
CREATE TABLE IF NOT EXISTS interview_phase_configs (
    id                      BIGINT              NOT NULL,
    template_id             BIGINT              NOT NULL,
    phase_code              VARCHAR(32)         NOT NULL,
    question_count          INT                 NOT NULL DEFAULT 3,
    difficulty_weight       DOUBLE PRECISION    NOT NULL DEFAULT 0.5,
    prompt_override         TEXT,
    version                 INT                 NOT NULL DEFAULT 0,
    is_deleted              BOOLEAN             NOT NULL DEFAULT FALSE,
    updated_at              TIMESTAMPTZ         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_interview_phase_question_count
        CHECK (question_count BETWEEN 1 AND 20),
    CONSTRAINT ck_interview_phase_difficulty
        CHECK (difficulty_weight BETWEEN 0.0 AND 1.0),
    CONSTRAINT ck_interview_phase_code
        CHECK (phase_code ~ '^[A-Z][A-Z0-9_]{0,31}$'),
    CONSTRAINT ck_interview_phase_version
        CHECK (version >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_phase_template_code
    ON interview_phase_configs (template_id, phase_code)
    WHERE is_deleted = FALSE;

COMMENT ON TABLE interview_phase_configs IS 'AI动态组卷阶段策略配置表';
COMMENT ON COLUMN interview_phase_configs.id IS '雪花主键';
COMMENT ON COLUMN interview_phase_configs.template_id IS '[逻辑外键]→interview_stage_templates';
COMMENT ON COLUMN interview_phase_configs.phase_code IS '模板内的阶段编码';
COMMENT ON COLUMN interview_phase_configs.version IS '多管理员编辑使用的乐观锁版本号';
COMMENT ON COLUMN interview_phase_configs.is_deleted IS '逻辑删除标识';

-- ==================== 3. interview_plan_drafts ====================
CREATE TABLE IF NOT EXISTS interview_plan_drafts (
    id                      BIGINT          NOT NULL,
    enterprise_id           BIGINT          NOT NULL,
    application_id          BIGINT          NOT NULL,
    template_id             BIGINT          NOT NULL,
    requested_by            BIGINT          NOT NULL,
    idempotency_key         VARCHAR(128)    NOT NULL,
    request_json            JSONB           NOT NULL,
    input_snapshot_json     JSONB,
    plan_json               JSONB,
    status                  VARCHAR(16)     NOT NULL DEFAULT 'PENDING',
    failure_reason          VARCHAR(512),
    generation_message_id   BIGINT,
    version                 INT             NOT NULL DEFAULT 0,
    expires_at              TIMESTAMPTZ,
    applied_at              TIMESTAMPTZ,
    apply_idempotency_key   VARCHAR(128),
    applied_plan_json       JSONB,
    applied_schedule_ids    JSONB,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by              BIGINT,
    trace_id                VARCHAR(128),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_interview_plan_request_object
        CHECK (jsonb_typeof(request_json) = 'object'),
    CONSTRAINT ck_interview_plan_input_object
        CHECK (input_snapshot_json IS NULL OR jsonb_typeof(input_snapshot_json) = 'object'),
    CONSTRAINT ck_interview_plan_result_object
        CHECK (plan_json IS NULL OR jsonb_typeof(plan_json) = 'object'),
    CONSTRAINT ck_interview_plan_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'READY', 'APPLIED', 'FAILED', 'EXPIRED')),
    CONSTRAINT ck_interview_plan_version
        CHECK (version >= 0),
    CONSTRAINT ck_interview_plan_applied_object
        CHECK (applied_plan_json IS NULL OR jsonb_typeof(applied_plan_json) = 'object'),
    CONSTRAINT ck_interview_plan_applied_ids
        CHECK (applied_schedule_ids IS NULL OR jsonb_typeof(applied_schedule_ids) = 'array')
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_plan_idempotency
    ON interview_plan_drafts (enterprise_id, application_id, requested_by, idempotency_key);
CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_plan_application_active
    ON interview_plan_drafts (application_id)
    WHERE status IN ('PENDING', 'PROCESSING', 'READY');
CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_plan_generation_message
    ON interview_plan_drafts (generation_message_id)
    WHERE generation_message_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_interview_plan_application_created
    ON interview_plan_drafts (enterprise_id, application_id, created_at DESC, id DESC);

COMMENT ON TABLE interview_plan_drafts IS 'Agent 面试编排业务草案表';
COMMENT ON COLUMN interview_plan_drafts.application_id IS '[逻辑外键]→job_applications';
COMMENT ON COLUMN interview_plan_drafts.template_id IS '[逻辑外键]→interview_stage_templates';
COMMENT ON COLUMN interview_plan_drafts.request_json IS '创建草案时的编排参数快照';
COMMENT ON COLUMN interview_plan_drafts.input_snapshot_json IS 'Agent 使用的非敏感输入快照';
COMMENT ON COLUMN interview_plan_drafts.plan_json IS '阶段、题纲和排期建议结构化结果';
COMMENT ON COLUMN interview_plan_drafts.generation_message_id IS '[逻辑引用]→local_message，不建跨模块外键';
COMMENT ON COLUMN interview_plan_drafts.version IS 'HR 确认草案使用的乐观锁版本';
COMMENT ON COLUMN interview_plan_drafts.apply_idempotency_key IS 'HR 应用草案时的幂等键，仅 APPLIED 后有值';
COMMENT ON COLUMN interview_plan_drafts.applied_plan_json IS 'HR 应用时提交的修订后计划快照，用于幂等对比与页面刷新恢复';
COMMENT ON COLUMN interview_plan_drafts.applied_schedule_ids IS '应用时创建的排期 ID 数组快照，用于幂等重放';

-- ==================== 4. candidate_interview_availability ====================
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

-- ==================== 5. interview_schedule ====================
CREATE TABLE IF NOT EXISTS interview_schedule (
    id                      BIGINT          NOT NULL,
    enterprise_id           BIGINT          NOT NULL,
    company_user_id         BIGINT          NOT NULL,
    interviewer_user_id     BIGINT,
    application_id          BIGINT          NOT NULL,
    template_id             BIGINT          NOT NULL,
    round_no                SMALLINT        NOT NULL DEFAULT 1,
    phase_code              VARCHAR(32)     NOT NULL,
    template_snapshot_json  JSONB           NOT NULL,
    idempotency_key         VARCHAR(128)    NOT NULL,
    interview_time          TIMESTAMPTZ     NOT NULL,
    duration_minutes        INT             NOT NULL DEFAULT 60,
    interview_type          VARCHAR(16)     NOT NULL DEFAULT 'TEXT',
    status                  VARCHAR(32)     NOT NULL DEFAULT 'PENDING_CONFIRMATION',
    status_reason           VARCHAR(256),
    version                 INT             NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted              BOOLEAN         NOT NULL DEFAULT FALSE,
    updated_by              BIGINT,
    trace_id                VARCHAR(128),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_interview_schedule_round
        CHECK (round_no > 0),
    CONSTRAINT ck_interview_schedule_phase_code
        CHECK (phase_code ~ '^[A-Z][A-Z0-9_]{0,31}$'),
    CONSTRAINT ck_interview_schedule_template_snapshot
        CHECK (jsonb_typeof(template_snapshot_json) = 'object'),
    CONSTRAINT ck_interview_schedule_duration
        CHECK (duration_minutes BETWEEN 15 AND 480),
    CONSTRAINT ck_interview_schedule_type
        CHECK (interview_type IN ('TEXT', 'VOICE', 'CODE')),
    CONSTRAINT ck_interview_schedule_status
        CHECK (status IN (
            'PENDING_CONFIRMATION', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED',
            'DECLINED', 'CANCELLED', 'NO_SHOW'
        )),
    CONSTRAINT ck_interview_schedule_version
        CHECK (version >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_schedule_application_round_active
    ON interview_schedule (application_id, round_no)
    WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_schedule_application_phase_active
    ON interview_schedule (application_id, phase_code)
    WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_schedule_idempotency
    ON interview_schedule (enterprise_id, idempotency_key);
CREATE INDEX IF NOT EXISTS idx_interview_schedule_enterprise_time
    ON interview_schedule (enterprise_id, interview_time, id)
    WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_interview_schedule_interviewer_time
    ON interview_schedule (interviewer_user_id, interview_time, id)
    WHERE is_deleted = FALSE
      AND interviewer_user_id IS NOT NULL
      AND status IN ('PENDING_CONFIRMATION', 'CONFIRMED', 'IN_PROGRESS');
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
CREATE INDEX IF NOT EXISTS idx_interview_schedule_application
    ON interview_schedule (application_id, round_no DESC);

COMMENT ON TABLE interview_schedule IS '单轮面试排期及面试生命周期表';
COMMENT ON COLUMN interview_schedule.id IS '雪花主键';
COMMENT ON COLUMN interview_schedule.enterprise_id IS '[逻辑外键]→enterprises';
COMMENT ON COLUMN interview_schedule.company_user_id IS '[逻辑外键]→sys_users，创建排期的企业用户';
COMMENT ON COLUMN interview_schedule.interviewer_user_id IS '[逻辑外键]→sys_users，被分配的面试官';
COMMENT ON COLUMN interview_schedule.application_id IS '[逻辑外键]→job_applications，候选人和岗位由投递记录确定';
COMMENT ON COLUMN interview_schedule.template_id IS '[逻辑外键]→interview_stage_templates';
COMMENT ON COLUMN interview_schedule.round_no IS '投递下的面试轮次，对应模板阶段 sortOrder';
COMMENT ON COLUMN interview_schedule.phase_code IS '由模板快照按 round_no 派生的阶段编码';
COMMENT ON COLUMN interview_schedule.template_snapshot_json IS '首轮排期固化的完整模板及阶段组卷配置快照';
COMMENT ON COLUMN interview_schedule.idempotency_key IS '创建排期请求幂等键';
COMMENT ON COLUMN interview_schedule.status IS 'PENDING_CONFIRMATION / CONFIRMED / IN_PROGRESS / COMPLETED / DECLINED / CANCELLED / NO_SHOW';
COMMENT ON COLUMN interview_schedule.version IS '排期管理操作使用的乐观锁版本号';
COMMENT ON COLUMN interview_schedule.updated_by IS '[逻辑外键]→sys_users';

-- ==================== 6. interview_sessions ====================
CREATE TABLE IF NOT EXISTS interview_sessions (
    id                      BIGINT          NOT NULL,
    enterprise_id           BIGINT          NOT NULL,
    user_id                 BIGINT          NOT NULL,
    schedule_id             BIGINT          NOT NULL,
    attempt_no              SMALLINT        NOT NULL DEFAULT 1,
    session_type            VARCHAR(16)     NOT NULL,
    idempotency_key         VARCHAR(128)    NOT NULL,
    total_questions         INT,
    current_question_index  INT             NOT NULL DEFAULT 0,
    last_event_sequence     BIGINT          NOT NULL DEFAULT 0,
    status                  VARCHAR(32)     NOT NULL DEFAULT 'CREATED',
    started_at              TIMESTAMPTZ,
    ended_at                TIMESTAMPTZ,
    termination_reason      VARCHAR(256),
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted              BOOLEAN         NOT NULL DEFAULT FALSE,
    trace_id                VARCHAR(128),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_interview_session_attempt
        CHECK (attempt_no > 0),
    CONSTRAINT ck_interview_session_type
        CHECK (session_type IN ('TEXT', 'VOICE', 'CODE')),
    CONSTRAINT ck_interview_session_question_count
        CHECK (total_questions IS NULL OR total_questions > 0),
    CONSTRAINT ck_interview_session_question_index
        CHECK (current_question_index >= 0),
    CONSTRAINT ck_interview_session_event_sequence
        CHECK (last_event_sequence >= 0),
    CONSTRAINT ck_interview_session_status
        CHECK (status IN ('CREATED', 'IN_PROGRESS', 'COMPLETED', 'TERMINATED')),
    CONSTRAINT ck_interview_session_time_order
        CHECK (ended_at IS NULL OR started_at IS NULL OR ended_at >= started_at)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_session_schedule_attempt_active
    ON interview_sessions (schedule_id, attempt_no)
    WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_session_idempotency
    ON interview_sessions (schedule_id, idempotency_key);
CREATE INDEX IF NOT EXISTS idx_interview_session_user_created
    ON interview_sessions (user_id, created_at DESC, id DESC)
    WHERE is_deleted = FALSE;

COMMENT ON TABLE interview_sessions IS '多模态面试统一运行时会话表';
COMMENT ON COLUMN interview_sessions.id IS '雪花主键';
COMMENT ON COLUMN interview_sessions.enterprise_id IS '[逻辑外键]→enterprises';
COMMENT ON COLUMN interview_sessions.user_id IS '[逻辑外键]→sys_users，候选人用户';
COMMENT ON COLUMN interview_sessions.schedule_id IS '[逻辑外键]→interview_schedule';
COMMENT ON COLUMN interview_sessions.idempotency_key IS '创建会话请求幂等键';
COMMENT ON COLUMN interview_sessions.last_event_sequence IS '已持久化语义事件的最后序号，使用原子条件更新推进';

-- ==================== 7. interview_answers ====================
CREATE TABLE IF NOT EXISTS interview_answers (
    id                      BIGINT          NOT NULL,
    session_id              BIGINT          NOT NULL,
    enterprise_id           BIGINT          NOT NULL,
    question_index          INT             NOT NULL,
    question_text           TEXT            NOT NULL,
    parent_message_id       BIGINT,
    follow_up_depth         SMALLINT        NOT NULL DEFAULT 0,
    idempotency_key         VARCHAR(128),
    user_answer             TEXT,
    score                   INT,
    ai_feedback             TEXT,
    answered_at             TIMESTAMPTZ,
    trace_id                VARCHAR(128),
    is_deleted              BOOLEAN         NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT ck_interview_answer_question_index
        CHECK (question_index >= 0),
    CONSTRAINT ck_interview_answer_follow_up_depth
        CHECK (follow_up_depth BETWEEN 0 AND 3),
    CONSTRAINT ck_interview_answer_score
        CHECK (score IS NULL OR score BETWEEN 0 AND 100),
    CONSTRAINT ck_interview_answer_answered
        CHECK (answered_at IS NULL OR user_answer IS NOT NULL)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_answer_idempotency
    ON interview_answers (session_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_interview_answer_session_order
    ON interview_answers (session_id, question_index, id)
    WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_interview_answer_parent
    ON interview_answers (parent_message_id)
    WHERE parent_message_id IS NOT NULL AND is_deleted = FALSE;

COMMENT ON TABLE interview_answers IS '文本面试问题、回答及单题AI评估记录表';
COMMENT ON COLUMN interview_answers.id IS '雪花主键，同时作为对外 questionId';
COMMENT ON COLUMN interview_answers.session_id IS '[逻辑外键]→interview_sessions';
COMMENT ON COLUMN interview_answers.enterprise_id IS '[逻辑外键]→enterprises';
COMMENT ON COLUMN interview_answers.parent_message_id IS '[逻辑外键]→interview_answers，追问所属的上一条消息';
COMMENT ON COLUMN interview_answers.idempotency_key IS '提交答案请求幂等键，题目未作答时为空';
COMMENT ON COLUMN interview_answers.answered_at IS '实际提交答案时间，题目生成后尚未回答时为空';

-- ==================== 8. interview_timeline_events ====================
CREATE TABLE IF NOT EXISTS interview_timeline_events (
    id                      BIGINT          NOT NULL,
    session_id              BIGINT          NOT NULL,
    enterprise_id           BIGINT          NOT NULL,
    event_id                VARCHAR(64)     NOT NULL,
    sequence_num            BIGINT          NOT NULL,
    event_type              VARCHAR(32)     NOT NULL,
    actor_type              VARCHAR(16)     NOT NULL,
    actor_user_id           BIGINT,
    payload_json            JSONB           NOT NULL,
    occurred_at             TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trace_id                VARCHAR(128),
    PRIMARY KEY (id),
    CONSTRAINT ck_interview_timeline_sequence
        CHECK (sequence_num > 0),
    CONSTRAINT ck_interview_timeline_actor
        CHECK (actor_type IN ('AI', 'CANDIDATE', 'INTERVIEWER', 'SYSTEM')),
    CONSTRAINT ck_interview_timeline_payload
        CHECK (jsonb_typeof(payload_json) = 'object')
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_timeline_session_sequence
    ON interview_timeline_events (session_id, sequence_num);
CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_timeline_session_event
    ON interview_timeline_events (session_id, event_id);
CREATE INDEX IF NOT EXISTS idx_interview_timeline_session_time
    ON interview_timeline_events (session_id, occurred_at, id);

COMMENT ON TABLE interview_timeline_events IS '面试断线补发与完整回放使用的语义事件时间线';
COMMENT ON COLUMN interview_timeline_events.id IS '雪花主键';
COMMENT ON COLUMN interview_timeline_events.session_id IS '[逻辑外键]→interview_sessions';
COMMENT ON COLUMN interview_timeline_events.enterprise_id IS '[逻辑外键]→enterprises';
COMMENT ON COLUMN interview_timeline_events.event_id IS '会话内事件幂等ID';
COMMENT ON COLUMN interview_timeline_events.sequence_num IS '会话内严格递增序号';
COMMENT ON COLUMN interview_timeline_events.payload_json IS '完整语义事件载荷，不保存高频增量片段或心跳';

-- ==================== 9. interview_takeovers ====================
CREATE TABLE IF NOT EXISTS interview_takeovers (
    id                      BIGINT          NOT NULL,
    session_id              BIGINT          NOT NULL,
    enterprise_id           BIGINT          NOT NULL,
    interviewer_user_id     BIGINT          NOT NULL,
    request_id              VARCHAR(64)     NOT NULL,
    status                  VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    reason                  VARCHAR(256),
    started_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at                TIMESTAMPTZ,
    trace_id                VARCHAR(128),
    PRIMARY KEY (id),
    CONSTRAINT ck_interview_takeover_status
        CHECK (status IN ('ACTIVE', 'ENDED', 'REVOKED')),
    CONSTRAINT ck_interview_takeover_time_order
        CHECK (ended_at IS NULL OR ended_at >= started_at)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_takeover_request
    ON interview_takeovers (session_id, request_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_takeover_active
    ON interview_takeovers (session_id)
    WHERE status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_interview_takeover_session_time
    ON interview_takeovers (session_id, started_at DESC, id DESC);

COMMENT ON TABLE interview_takeovers IS '面试官人工接管会话记录表';
COMMENT ON COLUMN interview_takeovers.id IS '雪花主键';
COMMENT ON COLUMN interview_takeovers.session_id IS '[逻辑外键]→interview_sessions';
COMMENT ON COLUMN interview_takeovers.enterprise_id IS '[逻辑外键]→enterprises';
COMMENT ON COLUMN interview_takeovers.interviewer_user_id IS '[逻辑外键]→sys_users';
COMMENT ON COLUMN interview_takeovers.request_id IS '接管命令幂等ID';

-- ==================== 10. interview_reports ====================
CREATE TABLE IF NOT EXISTS interview_reports (
    id                      BIGINT          NOT NULL,
    enterprise_id           BIGINT          NOT NULL,
    schedule_id             BIGINT          NOT NULL,
    generation_status       VARCHAR(16)     NOT NULL DEFAULT 'PENDING',
    failure_reason          VARCHAR(512),
    overall_ai_score        INT,
    executive_summary       TEXT,
    code_capability_review  TEXT,
    communication_score     INT,
    report_pdf_url          VARCHAR(512),
    completed_at            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trace_id                VARCHAR(128),
    is_deleted              BOOLEAN         NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT ck_interview_report_status
        CHECK (generation_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_interview_report_overall_score
        CHECK (overall_ai_score IS NULL OR overall_ai_score BETWEEN 0 AND 100),
    CONSTRAINT ck_interview_report_communication_score
        CHECK (communication_score IS NULL OR communication_score BETWEEN 0 AND 100),
    CONSTRAINT ck_interview_report_completion
        CHECK (
            (generation_status = 'COMPLETED' AND completed_at IS NOT NULL)
            OR generation_status <> 'COMPLETED'
        )
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_report_schedule_active
    ON interview_reports (schedule_id)
    WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_interview_report_enterprise_status
    ON interview_reports (enterprise_id, generation_status, created_at DESC, id DESC)
    WHERE is_deleted = FALSE;

COMMENT ON TABLE interview_reports IS '面试报告生成状态及最终报告内容表';
COMMENT ON COLUMN interview_reports.id IS '雪花主键';
COMMENT ON COLUMN interview_reports.enterprise_id IS '[逻辑外键]→enterprises';
COMMENT ON COLUMN interview_reports.schedule_id IS '[逻辑外键]→interview_schedule';
COMMENT ON COLUMN interview_reports.generation_status IS 'PENDING / PROCESSING / COMPLETED / FAILED';
COMMENT ON COLUMN interview_reports.failure_reason IS '报告生成失败的受控原因';
COMMENT ON COLUMN interview_reports.report_pdf_url IS '报告PDF对象存储定位信息，不直接作为永久公网地址使用';

-- ==================== 11. application_transition_logs ====================
CREATE TABLE IF NOT EXISTS application_transition_logs (
    id                      BIGINT          NOT NULL,
    enterprise_id           BIGINT          NOT NULL,
    application_id          BIGINT          NOT NULL,
    from_status             VARCHAR(32)     NOT NULL,
    to_status               VARCHAR(32)     NOT NULL,
    operator_user_id        BIGINT          NOT NULL,
    transition_reason       VARCHAR(256),
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trace_id                VARCHAR(128),
    PRIMARY KEY (id),
    CONSTRAINT ck_application_transition_changed
        CHECK (from_status <> to_status)
);

CREATE INDEX IF NOT EXISTS idx_application_transition_application_time
    ON application_transition_logs (application_id, created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_application_transition_enterprise_time
    ON application_transition_logs (enterprise_id, created_at DESC, id DESC);

COMMENT ON TABLE application_transition_logs IS '投递聚合的招聘状态流转历史日志';
COMMENT ON COLUMN application_transition_logs.id IS '雪花主键';
COMMENT ON COLUMN application_transition_logs.enterprise_id IS '[逻辑外键]→enterprises，冗余租户隔离键';
COMMENT ON COLUMN application_transition_logs.application_id IS '[逻辑外键]→job_applications';
COMMENT ON COLUMN application_transition_logs.operator_user_id IS '[逻辑外键]→sys_users，系统自动推进时为0';

-- ==================== 12. offers ====================
CREATE TABLE IF NOT EXISTS offers (
    id                          BIGINT          NOT NULL,
    enterprise_id               BIGINT          NOT NULL,
    application_id              BIGINT          NOT NULL,
    candidate_user_id           BIGINT          NOT NULL,
    title                       VARCHAR(128)    NOT NULL,
    salary_min                  NUMERIC(12, 2),
    salary_max                  NUMERIC(12, 2),
    currency                    VARCHAR(3)      NOT NULL DEFAULT 'CNY',
    planned_start_date          DATE,
    content                     TEXT            NOT NULL,
    expires_at                  TIMESTAMPTZ     NOT NULL,
    status                      VARCHAR(16)     NOT NULL DEFAULT 'DRAFT',
    create_idempotency_key      VARCHAR(128)    NOT NULL,
    send_idempotency_key        VARCHAR(128),
    decision_idempotency_key    VARCHAR(128),
    decision_reason             VARCHAR(256),
    withdraw_reason             VARCHAR(256),
    sent_at                     TIMESTAMPTZ,
    decided_at                  TIMESTAMPTZ,
    withdrawn_at                TIMESTAMPTZ,
    version                     INT             NOT NULL DEFAULT 0,
    created_by                  BIGINT          NOT NULL,
    updated_by                  BIGINT,
    trace_id                    VARCHAR(128),
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_offers_status
        CHECK (status IN ('DRAFT', 'SENT', 'ACCEPTED', 'DECLINED', 'WITHDRAWN', 'EXPIRED')),
    CONSTRAINT ck_offers_salary
        CHECK (
            (salary_min IS NULL AND salary_max IS NULL)
            OR (
                salary_min IS NOT NULL
                AND salary_max IS NOT NULL
                AND salary_min >= 0
                AND salary_max >= salary_min
            )
        ),
    CONSTRAINT ck_offers_currency
        CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_offers_title
        CHECK (btrim(title) <> ''),
    CONSTRAINT ck_offers_content
        CHECK (btrim(content) <> ''),
    CONSTRAINT ck_offers_expiration
        CHECK (expires_at > created_at),
    CONSTRAINT ck_offers_version
        CHECK (version >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_offers_application_open_or_accepted
    ON offers (application_id)
    WHERE status IN ('DRAFT', 'SENT', 'ACCEPTED');
CREATE UNIQUE INDEX IF NOT EXISTS uk_offers_create_idempotency
    ON offers (enterprise_id, created_by, create_idempotency_key);
CREATE UNIQUE INDEX IF NOT EXISTS uk_offers_send_idempotency
    ON offers (enterprise_id, send_idempotency_key)
    WHERE send_idempotency_key IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_offers_decision_idempotency
    ON offers (candidate_user_id, decision_idempotency_key)
    WHERE decision_idempotency_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_offers_application_created
    ON offers (application_id, created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_offers_candidate_status_created
    ON offers (candidate_user_id, status, created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_offers_enterprise_status_created
    ON offers (enterprise_id, status, created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_offers_pending_expiration
    ON offers (expires_at, id)
    WHERE status = 'SENT';

COMMENT ON TABLE offers IS '企业向候选人发出的录用邀请及其决策状态';
COMMENT ON COLUMN offers.enterprise_id IS '[逻辑外键]→enterprises';
COMMENT ON COLUMN offers.application_id IS '[逻辑外键]→job_applications';
COMMENT ON COLUMN offers.candidate_user_id IS '[逻辑外键]→sys_users，冗余保存以支持候选人归属校验和查询';
COMMENT ON COLUMN offers.status IS 'DRAFT / SENT / ACCEPTED / DECLINED / WITHDRAWN / EXPIRED';
COMMENT ON COLUMN offers.version IS '修改、发送和撤回使用的乐观锁版本号';
COMMENT ON COLUMN offers.created_by IS '[逻辑外键]→sys_users，创建 Offer 的企业用户';

COMMIT;
