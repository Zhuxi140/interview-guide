-- ============================================================
-- Phase 9: 增值与运营 — 表结构初始化
-- 数据库: PostgreSQL 14+；幂等可重复执行；不使用物理外键；
-- 时间字段使用 TIMESTAMPTZ，布尔使用 BOOLEAN，JSON 使用 JSONB。
-- ============================================================

BEGIN;

-- ==================== 9.1 interview_calendar_slots ====================
-- 面试官日历空闲时段表；version 支撑 If-Match 条件删除，
-- created_at/updated_by/trace_id 为接口契约要求的审计字段。
CREATE TABLE IF NOT EXISTS interview_calendar_slots
(
    id                 BIGINT       PRIMARY KEY,
    enterprise_id      BIGINT       NOT NULL,
    interviewer_user_id BIGINT      NOT NULL,
    slot_start         TIMESTAMPTZ  NOT NULL,
    slot_end           TIMESTAMPTZ  NOT NULL,
    is_allocated       BOOLEAN      NOT NULL DEFAULT FALSE,
    version            INT          NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by         BIGINT,
    trace_id           VARCHAR(128),
    is_deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_calendar_slots_time CHECK (slot_start < slot_end)
);

COMMENT ON TABLE interview_calendar_slots IS '面试官日历空闲时段表';
COMMENT ON COLUMN interview_calendar_slots.id IS '雪花主键';
COMMENT ON COLUMN interview_calendar_slots.enterprise_id IS '[逻辑外键]→enterprises，关联企业租户 ID';
COMMENT ON COLUMN interview_calendar_slots.interviewer_user_id IS '[逻辑外键]→sys_users，面试官系统用户 ID';
COMMENT ON COLUMN interview_calendar_slots.slot_start IS '空闲时段开始时间';
COMMENT ON COLUMN interview_calendar_slots.slot_end IS '空闲时段结束时间';
COMMENT ON COLUMN interview_calendar_slots.is_allocated IS 'FALSE: 空闲开放中, TRUE: 已排期锁定';
COMMENT ON COLUMN interview_calendar_slots.version IS 'CAS 版本号；If-Match 条件删除使用';
COMMENT ON COLUMN interview_calendar_slots.created_at IS '创建时间';
COMMENT ON COLUMN interview_calendar_slots.updated_by IS '[逻辑外键]→sys_users，最后修改人';
COMMENT ON COLUMN interview_calendar_slots.trace_id IS '调用链 ID';
COMMENT ON COLUMN interview_calendar_slots.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN interview_calendar_slots.updated_at IS '更新时间';

CREATE INDEX IF NOT EXISTS idx_calendar_slots_interviewer_time
    ON interview_calendar_slots (interviewer_user_id, slot_start);
CREATE INDEX IF NOT EXISTS idx_calendar_slots_enterprise
    ON interview_calendar_slots (enterprise_id, slot_start);


-- ==================== 9.1 ai_tutor_sessions ====================
-- C 端智能考点答疑会话主表。
CREATE TABLE IF NOT EXISTS ai_tutor_sessions
(
    id                   BIGINT        PRIMARY KEY,
    enterprise_id        BIGINT        NOT NULL,
    user_id              BIGINT        NOT NULL,
    associated_report_id BIGINT        NOT NULL,
    session_title        VARCHAR(128)  NOT NULL,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trace_id             VARCHAR(128),
    is_deleted           BOOLEAN       NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE ai_tutor_sessions IS 'C 端智能考点答疑教研会话主表';
COMMENT ON COLUMN ai_tutor_sessions.id IS '雪花主键';
COMMENT ON COLUMN ai_tutor_sessions.enterprise_id IS '[逻辑外键]→enterprises，强隔离企业租户 ID';
COMMENT ON COLUMN ai_tutor_sessions.user_id IS '[逻辑外键]→sys_users，发起答疑的 C 端求职者 ID';
COMMENT ON COLUMN ai_tutor_sessions.associated_report_id IS '[逻辑外键]→interview_reports，强关联面评报告 ID';
COMMENT ON COLUMN ai_tutor_sessions.session_title IS '会话引导自拟标题';
COMMENT ON COLUMN ai_tutor_sessions.created_at IS '会话开启时间';
COMMENT ON COLUMN ai_tutor_sessions.updated_at IS '最后提问活跃时间';
COMMENT ON COLUMN ai_tutor_sessions.trace_id IS '会话创建调用链 ID';
COMMENT ON COLUMN ai_tutor_sessions.is_deleted IS '逻辑删除标识';

CREATE INDEX IF NOT EXISTS idx_tutor_sessions_user_time
    ON ai_tutor_sessions (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_tutor_sessions_report
    ON ai_tutor_sessions (associated_report_id);


-- ==================== 9.1 ai_tutor_messages ====================
-- AI 答疑交互消息表；用户消息与 AI 消息分开保存，id 单调递增支撑游标分页。
CREATE TABLE IF NOT EXISTS ai_tutor_messages
(
    id           BIGINT       PRIMARY KEY,
    session_id   BIGINT       NOT NULL,
    message_type VARCHAR(16)  NOT NULL,
    content      TEXT         NOT NULL,
    trace_id     VARCHAR(128),
    is_deleted   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_tutor_messages_type CHECK (message_type IN ('USER', 'AI')),
    CONSTRAINT chk_tutor_messages_content CHECK (NULLIF(BTRIM(content), '') IS NOT NULL)
);

COMMENT ON TABLE ai_tutor_messages IS 'AI 答疑交互消息表';
COMMENT ON COLUMN ai_tutor_messages.id IS '雪花主键；游标分页游标';
COMMENT ON COLUMN ai_tutor_messages.session_id IS '[逻辑外键]→ai_tutor_sessions，关联答疑会话';
COMMENT ON COLUMN ai_tutor_messages.message_type IS '消息角色 (USER / AI)';
COMMENT ON COLUMN ai_tutor_messages.content IS '提问或回答正文';
COMMENT ON COLUMN ai_tutor_messages.trace_id IS '本次问答调用链 ID';
COMMENT ON COLUMN ai_tutor_messages.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN ai_tutor_messages.created_at IS '消息创建时间';

CREATE INDEX IF NOT EXISTS idx_tutor_messages_session_id
    ON ai_tutor_messages (session_id, id);


-- ==================== 9.2 sys_notifications ====================
-- 系统消息与投递记录表（含 email_logs/sms_logs 功能）。
-- 若库中已存在旧结构，先按原结构建表（IF NOT EXISTS 不生效时跳过），
-- 再以 ALTER TABLE ... ADD COLUMN IF NOT EXISTS 补扩列。
CREATE TABLE IF NOT EXISTS sys_notifications
(
    id             BIGINT        PRIMARY KEY,
    enterprise_id  BIGINT        NOT NULL,
    user_id        BIGINT        NOT NULL,
    notify_type    VARCHAR(32)   NOT NULL,
    title          VARCHAR(128)  NOT NULL,
    content        TEXT,
    is_read        BOOLEAN       NOT NULL DEFAULT FALSE,
    trace_id       VARCHAR(128),
    is_deleted     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2026-08-21 接口回填扩列：发送侧管理接口依赖以下列。
ALTER TABLE sys_notifications ADD COLUMN IF NOT EXISTS notify_scene VARCHAR(64);
ALTER TABLE sys_notifications ADD COLUMN IF NOT EXISTS channel_type VARCHAR(16) NOT NULL DEFAULT 'IN_APP';
ALTER TABLE sys_notifications ADD COLUMN IF NOT EXISTS send_status VARCHAR(20) NOT NULL DEFAULT 'SENT';
ALTER TABLE sys_notifications ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(512);
ALTER TABLE sys_notifications ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

COMMENT ON TABLE sys_notifications IS '系统消息与投递记录表（含 email_logs 功能）';
COMMENT ON COLUMN sys_notifications.id IS '雪花主键';
COMMENT ON COLUMN sys_notifications.enterprise_id IS '[逻辑外键]→enterprises，强隔离企业租户 ID';
COMMENT ON COLUMN sys_notifications.user_id IS '[逻辑外键]→sys_users，接收消息的用户 ID';
COMMENT ON COLUMN sys_notifications.notify_type IS '类型 (SYSTEM / INTERVIEW / EMAIL_LOG / SMS_LOG)';
COMMENT ON COLUMN sys_notifications.notify_scene IS '业务场景 (INTERVIEW_INVITE / INTERVIEW_CANCEL / OFFER_SENT / OFFER_DECIDED / REPORT_READY / SYSTEM)';
COMMENT ON COLUMN sys_notifications.channel_type IS '发送渠道 (IN_APP / EMAIL / SMS)；IN_APP 不依赖外部渠道配置';
COMMENT ON COLUMN sys_notifications.send_status IS '发送状态 (PENDING / SENT / FAILED)；存量站内信数据视为 SENT';
COMMENT ON COLUMN sys_notifications.failure_reason IS '外部渠道发送失败原因';
COMMENT ON COLUMN sys_notifications.title IS '消息/邮件标题（模板渲染后）';
COMMENT ON COLUMN sys_notifications.content IS '消息/邮件正文内容（模板渲染后）';
COMMENT ON COLUMN sys_notifications.is_read IS '已读/送达状态';
COMMENT ON COLUMN sys_notifications.trace_id IS '触发通知的原始业务链路 ID';
COMMENT ON COLUMN sys_notifications.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN sys_notifications.created_at IS '发送时间';
COMMENT ON COLUMN sys_notifications.updated_at IS '重发/发送状态流转时间';

CREATE INDEX IF NOT EXISTS idx_notifications_user_created
    ON sys_notifications (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_channel_status
    ON sys_notifications (channel_type, send_status, created_at DESC);


-- ==================== 9.2 sys_notification_channels ====================
-- 通知发送渠道配置表；每渠道固定一行，凭证字段在 config_json 内加密存储。
CREATE TABLE IF NOT EXISTS sys_notification_channels
(
    id           BIGINT       PRIMARY KEY,
    channel_type VARCHAR(16)  NOT NULL,
    is_enabled   BOOLEAN      NOT NULL DEFAULT FALSE,
    provider     VARCHAR(64),
    config_json  JSONB,
    version      INT          NOT NULL DEFAULT 0,
    updated_by   BIGINT,
    trace_id     VARCHAR(128),
    is_deleted   BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_notification_channels_type CHECK (channel_type IN ('EMAIL', 'SMS', 'IN_APP'))
);

COMMENT ON TABLE sys_notification_channels IS '通知发送渠道配置表';
COMMENT ON COLUMN sys_notification_channels.id IS '雪花主键';
COMMENT ON COLUMN sys_notification_channels.channel_type IS '渠道类型 (EMAIL / SMS / IN_APP)；每渠道固定一行';
COMMENT ON COLUMN sys_notification_channels.is_enabled IS '渠道启用开关；IN_APP 行恒为 TRUE';
COMMENT ON COLUMN sys_notification_channels.provider IS '供应商标识（如 ALIYUN_SMS）';
COMMENT ON COLUMN sys_notification_channels.config_json IS '渠道参数；凭证字段加密存储，查询接口仅返回脱敏摘要';
COMMENT ON COLUMN sys_notification_channels.version IS 'CAS 版本号；PUT 更新携带 expectedVersion，零行更新返回 409';
COMMENT ON COLUMN sys_notification_channels.updated_by IS '[逻辑外键]→sys_users，最后修改人';
COMMENT ON COLUMN sys_notification_channels.trace_id IS '调用链 ID';
COMMENT ON COLUMN sys_notification_channels.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN sys_notification_channels.updated_at IS '更新时间';

-- 有效记录唯一索引（排除逻辑删除行），兜底并发创建。
CREATE UNIQUE INDEX IF NOT EXISTS uq_notification_channels_type
    ON sys_notification_channels (channel_type) WHERE is_deleted = FALSE;

-- 三渠道固定一行初始化；IN_APP 站内信恒为启用。
-- 种子行使用小字面主键，避免与应用侧雪花 ID 冲突。
INSERT INTO sys_notification_channels (id, channel_type, is_enabled)
VALUES (900001, 'IN_APP', TRUE), (900002, 'EMAIL', FALSE), (900003, 'SMS', FALSE)
ON CONFLICT DO NOTHING;


-- ==================== 9.2 sys_notification_templates ====================
-- 通知模板表；同一 (notify_scene, channel_type) 有效记录唯一。
CREATE TABLE IF NOT EXISTS sys_notification_templates
(
    id                BIGINT       PRIMARY KEY,
    notify_scene      VARCHAR(64)  NOT NULL,
    channel_type      VARCHAR(16)  NOT NULL,
    title             VARCHAR(128) NOT NULL,
    content_template  TEXT         NOT NULL,
    is_enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    version           INT          NOT NULL DEFAULT 0,
    updated_by        BIGINT,
    trace_id          VARCHAR(128),
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_notification_templates IS '通知模板表';
COMMENT ON COLUMN sys_notification_templates.id IS '雪花主键';
COMMENT ON COLUMN sys_notification_templates.notify_scene IS '业务场景，取值同 sys_notifications.notify_scene';
COMMENT ON COLUMN sys_notification_templates.channel_type IS '渠道 (EMAIL / SMS / IN_APP)';
COMMENT ON COLUMN sys_notification_templates.title IS '标题模板';
COMMENT ON COLUMN sys_notification_templates.content_template IS '正文模板；禁止客户端提交渲染后全文';
COMMENT ON COLUMN sys_notification_templates.is_enabled IS '模板启停';
COMMENT ON COLUMN sys_notification_templates.version IS 'CAS 版本号；PATCH 更新携带 expectedVersion';
COMMENT ON COLUMN sys_notification_templates.updated_by IS '[逻辑外键]→sys_users，最后修改人';
COMMENT ON COLUMN sys_notification_templates.trace_id IS '调用链 ID';
COMMENT ON COLUMN sys_notification_templates.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN sys_notification_templates.created_at IS '创建时间';
COMMENT ON COLUMN sys_notification_templates.updated_at IS '更新时间';

-- 有效记录唯一索引（排除逻辑删除行），兜底并发创建。
CREATE UNIQUE INDEX IF NOT EXISTS uq_notification_templates_scene_channel
    ON sys_notification_templates (notify_scene, channel_type) WHERE is_deleted = FALSE;


-- ==================== 9.3 sys_api_logs / sys_api_logs_archive ====================
-- API 高频访问日志热表与同构归档冷表。
CREATE TABLE IF NOT EXISTS sys_api_logs
(
    id               BIGINT       PRIMARY KEY,
    trace_id         VARCHAR(128) NOT NULL,
    user_id          BIGINT,
    api_url          VARCHAR(256) NOT NULL,
    request_method   VARCHAR(10)  NOT NULL,
    client_ip        VARCHAR(45),
    execution_time   BIGINT,
    response_status  INT,
    error_msg        VARCHAR(512),
    llm_input_tokens INT,
    llm_output_tokens INT,
    llm_model        VARCHAR(64),
    is_deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_api_logs IS 'API 高频访问日志热表';
COMMENT ON COLUMN sys_api_logs.id IS '雪花主键';
COMMENT ON COLUMN sys_api_logs.trace_id IS '分布式链路追踪 ID';
COMMENT ON COLUMN sys_api_logs.user_id IS '[逻辑外键]→sys_users，触发请求的操作人 ID';
COMMENT ON COLUMN sys_api_logs.api_url IS '请求的接口路径';
COMMENT ON COLUMN sys_api_logs.request_method IS 'HTTP 动词';
COMMENT ON COLUMN sys_api_logs.client_ip IS '客户端真实 IP 地址';
COMMENT ON COLUMN sys_api_logs.execution_time IS '接口执行耗时（毫秒）';
COMMENT ON COLUMN sys_api_logs.response_status IS 'HTTP 响应状态码';
COMMENT ON COLUMN sys_api_logs.error_msg IS '若接口报错，记录简要异常信息（脱敏）';
COMMENT ON COLUMN sys_api_logs.llm_input_tokens IS 'LLM 输入 Token 数（成本归因）';
COMMENT ON COLUMN sys_api_logs.llm_output_tokens IS 'LLM 输出 Token 数（成本归因）';
COMMENT ON COLUMN sys_api_logs.llm_model IS '使用的模型名称';
COMMENT ON COLUMN sys_api_logs.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN sys_api_logs.created_at IS '日志生成时间';

CREATE INDEX IF NOT EXISTS idx_api_logs_created_at ON sys_api_logs (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_api_logs_user_time ON sys_api_logs (user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS sys_api_logs_archive
(
    id               BIGINT       PRIMARY KEY,
    trace_id         VARCHAR(128) NOT NULL,
    user_id          BIGINT,
    api_url          VARCHAR(256) NOT NULL,
    request_method   VARCHAR(10)  NOT NULL,
    client_ip        VARCHAR(45),
    execution_time   BIGINT,
    response_status  INT,
    error_msg        VARCHAR(512),
    llm_input_tokens INT,
    llm_output_tokens INT,
    llm_model        VARCHAR(64),
    is_deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_api_logs_archive IS '运行日志历史归档冷表（结构同 sys_api_logs）';

CREATE INDEX IF NOT EXISTS idx_archive_logs_created_at
    ON sys_api_logs_archive (created_at DESC);


-- ==================== 9.3 sys_operate_logs ====================
-- 业务操作审计日志表；数据由 DataAuditInterceptor 产生。
CREATE TABLE IF NOT EXISTS sys_operate_logs
(
    id             BIGINT       PRIMARY KEY,
    trace_id       VARCHAR(128),
    user_id        BIGINT       NOT NULL,
    module         VARCHAR(64)  NOT NULL,
    operate_type   VARCHAR(16)  NOT NULL,
    target_table   VARCHAR(64),
    target_id      BIGINT,
    old_value_json JSONB,
    new_value_json JSONB,
    is_deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_operate_logs_type
        CHECK (operate_type IN ('INSERT', 'UPDATE', 'DELETE', 'GRANT'))
);

COMMENT ON TABLE sys_operate_logs IS '业务操作审计日志表（JSON 快照审计）';
COMMENT ON COLUMN sys_operate_logs.id IS '雪花主键';
COMMENT ON COLUMN sys_operate_logs.trace_id IS '关联同一次 HTTP 请求的链路 ID';
COMMENT ON COLUMN sys_operate_logs.user_id IS '[逻辑外键]→sys_users，操作人 ID';
COMMENT ON COLUMN sys_operate_logs.module IS '业务模块';
COMMENT ON COLUMN sys_operate_logs.operate_type IS '操作类型 (INSERT / UPDATE / DELETE / GRANT)';
COMMENT ON COLUMN sys_operate_logs.target_table IS '被修改的目标数据库表名';
COMMENT ON COLUMN sys_operate_logs.target_id IS '被修改的目标记录主键 ID';
COMMENT ON COLUMN sys_operate_logs.old_value_json IS '修改前的数据快照 (JSON，脱敏)';
COMMENT ON COLUMN sys_operate_logs.new_value_json IS '修改后的数据快照 (JSON，脱敏)';
COMMENT ON COLUMN sys_operate_logs.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN sys_operate_logs.created_at IS '操作发生时间';

CREATE INDEX IF NOT EXISTS idx_operate_table_time
    ON sys_operate_logs (target_table, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_operate_user_time
    ON sys_operate_logs (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_operate_biz
    ON sys_operate_logs (target_table, target_id);


-- ==================== 9.4 spark_corpus_tasks ====================
-- Spark 离线语料同步任务表。
CREATE TABLE IF NOT EXISTS spark_corpus_tasks
(
    id                        BIGINT       PRIMARY KEY,
    task_no                   VARCHAR(64)  NOT NULL,
    source_path               VARCHAR(512) NOT NULL,
    raw_count                 INT,
    cleaned_count             INT,
    chunk_count               INT,
    status                    VARCHAR(20),
    vector_db_write_latency_ms INT,
    embedding_api_latency_ms  INT,
    started_at                TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted                BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_by                BIGINT,
    trace_id                  VARCHAR(128),
    updated_at                TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_spark_tasks_task_no UNIQUE (task_no)
);

COMMENT ON TABLE spark_corpus_tasks IS 'Spark 离线语料同步任务表';
COMMENT ON COLUMN spark_corpus_tasks.id IS '雪花主键';
COMMENT ON COLUMN spark_corpus_tasks.task_no IS '批次任务唯一编号';
COMMENT ON COLUMN spark_corpus_tasks.source_path IS '数据源路径';
COMMENT ON COLUMN spark_corpus_tasks.raw_count IS '原始采集文本总条数';
COMMENT ON COLUMN spark_corpus_tasks.cleaned_count IS '清洗去噪后的有效条数';
COMMENT ON COLUMN spark_corpus_tasks.chunk_count IS '切片并成功向量化入库的 Chunk 总数';
COMMENT ON COLUMN spark_corpus_tasks.status IS '任务状态 (PENDING / RUNNING / SUCCESS / FAILED / CANCELLED)';
COMMENT ON COLUMN spark_corpus_tasks.vector_db_write_latency_ms IS '向量写入平均耗时（毫秒）';
COMMENT ON COLUMN spark_corpus_tasks.embedding_api_latency_ms IS 'Embedding 调用平均耗时（毫秒）';
COMMENT ON COLUMN spark_corpus_tasks.started_at IS '任务启动时间';
COMMENT ON COLUMN spark_corpus_tasks.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN spark_corpus_tasks.updated_by IS '[逻辑外键]→sys_users，操作人 ID';
COMMENT ON COLUMN spark_corpus_tasks.trace_id IS '调用链 ID';
COMMENT ON COLUMN spark_corpus_tasks.updated_at IS '更新时间';

CREATE INDEX IF NOT EXISTS idx_spark_tasks_status_time
    ON spark_corpus_tasks (status, started_at DESC);


-- ==================== 9.3 data_retention_policies ====================
-- 数据保留策略单行配置表（文档未定义 DDL，按接口契约设计）。
CREATE TABLE IF NOT EXISTS data_retention_policies
(
    id           BIGINT      PRIMARY KEY,
    policies_json JSONB,
    version      INT         NOT NULL DEFAULT 0,
    updated_by   BIGINT,
    trace_id     VARCHAR(128),
    is_deleted   BOOLEAN     NOT NULL DEFAULT FALSE,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_data_retention_single_row CHECK (id = 1)
);

COMMENT ON TABLE data_retention_policies IS '数据保留策略单行配置表（id 恒为 1）';
COMMENT ON COLUMN data_retention_policies.id IS '固定主键 1';
COMMENT ON COLUMN data_retention_policies.policies_json IS '策略数组 [{resourceType, hotRetentionDays, archiveEnabled, coldRetentionDays}]';
COMMENT ON COLUMN data_retention_policies.version IS '聚合 CAS 版本号';
COMMENT ON COLUMN data_retention_policies.updated_by IS '[逻辑外键]→sys_users，最后修改人';
COMMENT ON COLUMN data_retention_policies.trace_id IS '调用链 ID';
COMMENT ON COLUMN data_retention_policies.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN data_retention_policies.updated_at IS '更新时间';

-- 默认策略初始化：全部资源热保留 30 天、关闭归档。
INSERT INTO data_retention_policies (id, policies_json, version)
VALUES (1, '[{"resourceType":"API_LOG","hotRetentionDays":30,"archiveEnabled":true,"coldRetentionDays":365},
            {"resourceType":"OPERATE_LOG","hotRetentionDays":90,"archiveEnabled":true,"coldRetentionDays":365},
            {"resourceType":"INTERVIEW_TIMELINE","hotRetentionDays":180,"archiveEnabled":false},
            {"resourceType":"VOICE_MEDIA","hotRetentionDays":180,"archiveEnabled":false},
            {"resourceType":"INTERVIEW_REPORT","hotRetentionDays":365,"archiveEnabled":false}]'::jsonb, 0)
ON CONFLICT (id) DO NOTHING;


-- ==================== 9.3 data_archive_tasks ====================
-- 数据归档任务表（文档未定义 DDL，按接口契约设计）。
CREATE TABLE IF NOT EXISTS data_archive_tasks
(
    id              BIGINT       PRIMARY KEY,
    resource_type   VARCHAR(32)  NOT NULL,
    before_time     TIMESTAMPTZ,
    status          VARCHAR(20)  NOT NULL,
    archived_count  BIGINT       NOT NULL DEFAULT 0,
    failure_reason  VARCHAR(512),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at    TIMESTAMPTZ,
    updated_by      BIGINT,
    trace_id        VARCHAR(128),
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_archive_tasks_resource
        CHECK (resource_type IN ('API_LOG', 'OPERATE_LOG', 'INTERVIEW_TIMELINE', 'VOICE_MEDIA', 'INTERVIEW_REPORT')),
    CONSTRAINT chk_archive_tasks_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

COMMENT ON TABLE data_archive_tasks IS '数据归档任务表';
COMMENT ON COLUMN data_archive_tasks.id IS '雪花主键';
COMMENT ON COLUMN data_archive_tasks.resource_type IS '资源类型 (API_LOG / OPERATE_LOG / INTERVIEW_TIMELINE / VOICE_MEDIA / INTERVIEW_REPORT)';
COMMENT ON COLUMN data_archive_tasks.before_time IS '归档该时间之前的数据';
COMMENT ON COLUMN data_archive_tasks.status IS '任务状态 (PENDING / PROCESSING / COMPLETED / FAILED / CANCELLED)';
COMMENT ON COLUMN data_archive_tasks.archived_count IS '已归档条数';
COMMENT ON COLUMN data_archive_tasks.failure_reason IS '失败原因';
COMMENT ON COLUMN data_archive_tasks.created_at IS '创建时间';
COMMENT ON COLUMN data_archive_tasks.completed_at IS '完成时间';
COMMENT ON COLUMN data_archive_tasks.updated_by IS '[逻辑外键]→sys_users，操作人 ID';
COMMENT ON COLUMN data_archive_tasks.trace_id IS '调用链 ID';
COMMENT ON COLUMN data_archive_tasks.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN data_archive_tasks.updated_at IS '更新时间';

CREATE INDEX IF NOT EXISTS idx_archive_tasks_resource_status
    ON data_archive_tasks (resource_type, status, created_at DESC);

COMMIT;
