-- 第八阶段：风控与合规增强表结构初始化
-- PostgreSQL；跨模块关联仅使用逻辑外键，不创建物理 FOREIGN KEY。
-- 依据 docs/第8阶段/第八阶段数据库表设计.md 与 docs/databases.md 生成。
--
-- 说明：
-- 1. sys_user_kyc / sys_enterprise_cert 不建通用 version 列（databases.md 版本策略：
--    依据期望状态完成原子更新），审核 CAS 以 auth_status/audit_status = 待审核 为条件。
-- 2. sensitive_words / user_api_policies 为多管理员可编辑配置聚合，按 databases.md
--    版本策略建议增加 version INT NOT NULL DEFAULT 0，编辑/删除使用 expectedVersion 条件更新。
-- 3. 唯一约束统一挂在未删除行上（部分唯一索引），支持逻辑删除后重新提交/重建。
-- 4. anti_cheat_logs 客户端事件幂等：UNIQUE(session_id, client_event_id) 部分唯一索引，
--    批量上报接口落地后以此去重（本阶段仅建表）。
-- 5. sys_enterprise_cert 在表设计文档基础上增加 reject_reason VARCHAR(256)：
--    接口契约要求认证状态/详情返回拒绝原因（REJECT 审核必填原因），不落库无处承载。

BEGIN;

-- C 端个人实名认证表
CREATE TABLE IF NOT EXISTS sys_user_kyc
(
    id            BIGINT       PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    real_name     VARCHAR(64)  NOT NULL,
    id_card_no    VARCHAR(128) NOT NULL,
    face_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    auth_status   SMALLINT     NOT NULL DEFAULT 0,
    reject_reason VARCHAR(256),
    submit_time   TIMESTAMPTZ  NOT NULL,
    audit_time    TIMESTAMPTZ,
    is_deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    trace_id      VARCHAR(128),
    updated_at    TIMESTAMPTZ,
    CONSTRAINT chk_sys_user_kyc_auth_status
        CHECK (auth_status IN (0, 1, 2))
);

COMMENT ON TABLE sys_user_kyc IS 'C 端个人实名认证表；real_name/id_card_no 由应用层 AES 加密存储';
COMMENT ON COLUMN sys_user_kyc.id IS '雪花主键';
COMMENT ON COLUMN sys_user_kyc.user_id IS '逻辑关联 sys_users.id，关联的 C 端求职者（未删除行唯一）';
COMMENT ON COLUMN sys_user_kyc.real_name IS '真实姓名（密文存储）';
COMMENT ON COLUMN sys_user_kyc.id_card_no IS '身份证号（密文存储，未删除行唯一）';
COMMENT ON COLUMN sys_user_kyc.face_verified IS '活体人脸核验状态';
COMMENT ON COLUMN sys_user_kyc.auth_status IS '综合认证状态：0 待审核 / 1 已通过 / 2 已拒绝';
COMMENT ON COLUMN sys_user_kyc.reject_reason IS '审核拒绝原因';
COMMENT ON COLUMN sys_user_kyc.submit_time IS '首次/最近一次提交时间';
COMMENT ON COLUMN sys_user_kyc.audit_time IS '最终审核处理时间';

-- 一个用户仅一条有效实名记录；逻辑删除后可重新提交
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_kyc_user
    ON sys_user_kyc (user_id)
    WHERE is_deleted = FALSE;

-- 身份证号未删除行唯一（密文等值比较）
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_kyc_id_card
    ON sys_user_kyc (id_card_no)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_sys_user_kyc_status
    ON sys_user_kyc (auth_status, submit_time DESC)
    WHERE is_deleted = FALSE;

-- B 端企业主体资质认证表
CREATE TABLE IF NOT EXISTS sys_enterprise_cert
(
    id            BIGINT       PRIMARY KEY,
    enterprise_id BIGINT       NOT NULL,
    company_name  VARCHAR(128) NOT NULL,
    credit_code   VARCHAR(64)  NOT NULL,
    legal_person  VARCHAR(64)  NOT NULL,
    license_url   VARCHAR(512) NOT NULL,
    audit_status  SMALLINT     NOT NULL DEFAULT 0,
    reject_reason VARCHAR(256),
    auditor_id    BIGINT,
    submit_time   TIMESTAMPTZ  NOT NULL,
    audit_time    TIMESTAMPTZ,
    is_deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    trace_id      VARCHAR(128),
    updated_at    TIMESTAMPTZ,
    CONSTRAINT chk_sys_enterprise_cert_audit_status
        CHECK (audit_status IN (0, 1, 2))
);

COMMENT ON TABLE sys_enterprise_cert IS 'B 端企业主体资质认证表；company_name 为提交时从 enterprises 读取的快照';
COMMENT ON COLUMN sys_enterprise_cert.id IS '雪花主键';
COMMENT ON COLUMN sys_enterprise_cert.enterprise_id IS '逻辑关联 enterprises.id，未删除行唯一';
COMMENT ON COLUMN sys_enterprise_cert.credit_code IS '统一社会信用代码，未删除行唯一';
COMMENT ON COLUMN sys_enterprise_cert.legal_person IS '法定代表人姓名';
COMMENT ON COLUMN sys_enterprise_cert.license_url IS '营业执照扫描件存储路径（上传凭证签发的 materialToken）';
COMMENT ON COLUMN sys_enterprise_cert.audit_status IS '审核状态：0 待审核 / 1 已通过 / 2 已拒绝';
COMMENT ON COLUMN sys_enterprise_cert.reject_reason IS '审核拒绝原因（接口契约要求返回，表设计补充列）';
COMMENT ON COLUMN sys_enterprise_cert.auditor_id IS '逻辑关联 sys_users.id，平台审核专员';
COMMENT ON COLUMN sys_enterprise_cert.submit_time IS '资料提交时间';
COMMENT ON COLUMN sys_enterprise_cert.audit_time IS '审核处理时间';

-- 一个企业仅一条有效认证记录；拒绝后覆盖重报，逻辑删除后可重建
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_enterprise_cert_enterprise
    ON sys_enterprise_cert (enterprise_id)
    WHERE is_deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_enterprise_cert_credit_code
    ON sys_enterprise_cert (credit_code)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_sys_enterprise_cert_status
    ON sys_enterprise_cert (audit_status, submit_time DESC)
    WHERE is_deleted = FALSE;

-- 违规敏感词与防线配置表
CREATE TABLE IF NOT EXISTS sensitive_words
(
    id         BIGINT       PRIMARY KEY,
    word       VARCHAR(64)  NOT NULL,
    category   VARCHAR(32)  NOT NULL,
    action_type VARCHAR(16) NOT NULL DEFAULT 'BLOCK',
    version    INT          NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_by BIGINT,
    trace_id   VARCHAR(128),
    updated_at TIMESTAMPTZ,
    CONSTRAINT chk_sensitive_words_category
        CHECK (category IN ('POLITICAL', 'PROFANITY', 'CHEAT')),
    CONSTRAINT chk_sensitive_words_action_type
        CHECK (action_type IN ('BLOCK', 'REPLACE', 'ALERT')),
    CONSTRAINT chk_sensitive_words_version
        CHECK (version >= 0)
);

COMMENT ON TABLE sensitive_words IS '违规敏感词与防线配置表；管理端编辑/删除使用 expectedVersion 乐观锁';
COMMENT ON COLUMN sensitive_words.id IS '雪花主键';
COMMENT ON COLUMN sensitive_words.word IS '敏感词汇本体，未删除行唯一';
COMMENT ON COLUMN sensitive_words.category IS '类别：POLITICAL / PROFANITY / CHEAT';
COMMENT ON COLUMN sensitive_words.action_type IS '触发动作：BLOCK 拦截 / REPLACE 替换 / ALERT 告警';
COMMENT ON COLUMN sensitive_words.version IS '乐观锁版本号';
COMMENT ON COLUMN sensitive_words.updated_by IS '逻辑关联 sys_users.id，维护词库的运营 ID';

-- 逻辑删除后允许同词重建
CREATE UNIQUE INDEX IF NOT EXISTS uk_sensitive_words_word
    ON sensitive_words (word)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_sensitive_words_category
    ON sensitive_words (category, id DESC)
    WHERE is_deleted = FALSE;

-- 用户 API 策略表（限流/黑白名单）
CREATE TABLE IF NOT EXISTS user_api_policies
(
    id             BIGINT       PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    api_path_regex VARCHAR(256),
    policy_type    VARCHAR(32)  NOT NULL,
    limit_count    INT,
    limit_seconds  INT,
    action_type    SMALLINT     NOT NULL,
    expire_time    TIMESTAMPTZ,
    reason         VARCHAR(128),
    version        INT          NOT NULL DEFAULT 0,
    is_deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_user_api_policies_policy_type
        CHECK (policy_type IN ('RATE_LIMIT', 'BLACKLIST', 'WHITELIST')),
    CONSTRAINT chk_user_api_policies_action_type
        CHECK (action_type IN (1, -1)),
    CONSTRAINT chk_user_api_policies_version
        CHECK (version >= 0),
    CONSTRAINT chk_user_api_policies_rate_limit_params
        CHECK (
            (policy_type = 'RATE_LIMIT' AND limit_count IS NOT NULL AND limit_count > 0
                AND limit_seconds IS NOT NULL AND limit_seconds > 0)
            OR (policy_type IN ('BLACKLIST', 'WHITELIST')
                AND limit_count IS NULL AND limit_seconds IS NULL)
        )
);

COMMENT ON TABLE user_api_policies IS '用户 API 策略表（限流/黑白名单）；api_path_regex 为受限 Ant 风格模板，空表示匹配全部';
COMMENT ON COLUMN user_api_policies.id IS '雪花主键';
COMMENT ON COLUMN user_api_policies.user_id IS '逻辑关联 sys_users.id，被管控用户';
COMMENT ON COLUMN user_api_policies.policy_type IS '策略类型：RATE_LIMIT / BLACKLIST / WHITELIST';
COMMENT ON COLUMN user_api_policies.limit_count IS '时间窗口内允许次数（仅限流）';
COMMENT ON COLUMN user_api_policies.limit_seconds IS '时间窗口秒数（仅限流）';
COMMENT ON COLUMN user_api_policies.action_type IS '动作：1 强制放行 / -1 强制拦截';
COMMENT ON COLUMN user_api_policies.expire_time IS '策略过期时间，为空则永久';
COMMENT ON COLUMN user_api_policies.reason IS '风控原因备注';
COMMENT ON COLUMN user_api_policies.version IS '乐观锁版本号';

CREATE INDEX IF NOT EXISTS idx_user_api_policies_user
    ON user_api_policies (user_id, id DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_user_api_policies_type
    ON user_api_policies (policy_type, id DESC)
    WHERE is_deleted = FALSE;

-- 智能防作弊行为检测日志表
CREATE TABLE IF NOT EXISTS anti_cheat_logs
(
    id               BIGINT       PRIMARY KEY,
    enterprise_id    BIGINT       NOT NULL,
    session_id       BIGINT       NOT NULL,
    user_id          BIGINT       NOT NULL,
    client_event_id  VARCHAR(64)  NOT NULL,
    event_type       VARCHAR(32)  NOT NULL,
    duration_ms      BIGINT,
    snapshot_oss_url VARCHAR(512),
    trace_id         VARCHAR(128),
    is_deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_anti_cheat_logs_event_type
        CHECK (event_type IN ('PAGE_BLUR', 'NO_FACE', 'MULTI_FACE'))
);

COMMENT ON TABLE anti_cheat_logs IS '智能防作弊行为检测日志表；客户端信号仅为风险信号，不单独作为处罚结论';
COMMENT ON COLUMN anti_cheat_logs.id IS '雪花主键';
COMMENT ON COLUMN anti_cheat_logs.enterprise_id IS '逻辑关联 enterprises.id';
COMMENT ON COLUMN anti_cheat_logs.session_id IS '逻辑关联 interview_sessions.id（多态：文本/语音会话）';
COMMENT ON COLUMN anti_cheat_logs.user_id IS '逻辑关联 sys_users.id，涉嫌异常的候选人';
COMMENT ON COLUMN anti_cheat_logs.client_event_id IS '客户端事件幂等 ID，(session_id, client_event_id) 去重';
COMMENT ON COLUMN anti_cheat_logs.event_type IS '违规类型：PAGE_BLUR / NO_FACE / MULTI_FACE';
COMMENT ON COLUMN anti_cheat_logs.duration_ms IS '异常行为持续时间（毫秒）';
COMMENT ON COLUMN anti_cheat_logs.snapshot_oss_url IS '抓拍的现场暗灰度图像存储路径';
COMMENT ON COLUMN anti_cheat_logs.created_at IS '违规发生时间';

-- 客户端事件幂等：同一会话同一 eventId 仅记录一次
CREATE UNIQUE INDEX IF NOT EXISTS uk_anti_cheat_logs_session_event
    ON anti_cheat_logs (session_id, client_event_id)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_anti_cheat_logs_enterprise_time
    ON anti_cheat_logs (enterprise_id, created_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_anti_cheat_logs_filters
    ON anti_cheat_logs (event_type, user_id, session_id, created_at DESC)
    WHERE is_deleted = FALSE;

COMMIT;
