-- ============================================================
-- AICore：补齐 LLM 路由版本字段并新增场景执行参数表
-- PostgreSQL 14+
-- ============================================================

BEGIN;

-- 补齐 Provider 管理端 CAS 与审计字段。
ALTER TABLE llm_provider_config
    ADD COLUMN IF NOT EXISTS version INT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT,
    ADD COLUMN IF NOT EXISTS trace_id VARCHAR(128),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

UPDATE llm_provider_config
SET version = COALESCE(version, 0),
    updated_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP);

ALTER TABLE llm_provider_config
    ALTER COLUMN enabled SET DEFAULT FALSE,
    ALTER COLUMN version SET DEFAULT 0,
    ALTER COLUMN version SET NOT NULL,
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN is_deleted SET DEFAULT FALSE,
    ALTER COLUMN is_deleted SET NOT NULL,
    ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN updated_at SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'llm_provider_config'::regclass
          AND conname = 'chk_llm_provider_version'
    ) THEN
        ALTER TABLE llm_provider_config
            ADD CONSTRAINT chk_llm_provider_version CHECK (version >= 0);
    END IF;
END
$$;

COMMENT ON COLUMN llm_provider_config.api_key_ciphertext IS
    'AES-GCM 密文信封，包含格式版本、密钥 ID、随机 IV、密文和认证标签';
COMMENT ON COLUMN llm_provider_config.version IS '管理端 CAS 版本';
COMMENT ON COLUMN llm_provider_config.updated_by IS '[逻辑外键]→sys_users，最后修改人';
COMMENT ON COLUMN llm_provider_config.trace_id IS '最后一次修改的调用链 ID';
COMMENT ON COLUMN llm_provider_config.updated_at IS '最后更新时间';

-- 补齐全局单例配置的 CAS 与审计字段。
ALTER TABLE llm_global_setting
    ADD COLUMN IF NOT EXISTS version INT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT,
    ADD COLUMN IF NOT EXISTS trace_id VARCHAR(128);

UPDATE llm_global_setting
SET version = COALESCE(version, 0),
    updated_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP);

ALTER TABLE llm_global_setting
    ALTER COLUMN version SET DEFAULT 0,
    ALTER COLUMN version SET NOT NULL,
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN updated_at SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'llm_global_setting'::regclass
          AND conname = 'chk_llm_global_setting_singleton'
    ) THEN
        ALTER TABLE llm_global_setting
            ADD CONSTRAINT chk_llm_global_setting_singleton CHECK (id = 1);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'llm_global_setting'::regclass
          AND conname = 'chk_llm_global_setting_version'
    ) THEN
        ALTER TABLE llm_global_setting
            ADD CONSTRAINT chk_llm_global_setting_version CHECK (version >= 0);
    END IF;
END
$$;

COMMENT ON COLUMN llm_global_setting.version IS '管理端 CAS 版本';
COMMENT ON COLUMN llm_global_setting.updated_by IS '[逻辑外键]→sys_users，最后修改人';
COMMENT ON COLUMN llm_global_setting.trace_id IS '最后一次修改的调用链 ID';
COMMENT ON COLUMN llm_global_setting.updated_at IS '最后更新时间';

INSERT INTO llm_global_setting (
    id,
    default_chat_provider_id,
    default_embedding_provider_id,
    version,
    created_at,
    updated_at
) VALUES (
    1,
    NULL,
    NULL,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

-- 场景身份由代码与初始化数据维护，管理员只更新执行参数。
CREATE TABLE IF NOT EXISTS llm_scene_config (
    scene_code          VARCHAR(64)     NOT NULL,
    model_type          VARCHAR(16)     NOT NULL,
    provider_id         VARCHAR(64),
    temperature         NUMERIC(4,3),
    top_p               NUMERIC(4,3),
    max_input_tokens    INT             NOT NULL,
    max_output_tokens   INT             NOT NULL,
    timeout_seconds     INT             NOT NULL DEFAULT 60,
    prompt_version      VARCHAR(64)     NOT NULL,
    extra_options       JSONB           NOT NULL DEFAULT '{}'::jsonb,
    enabled             BOOLEAN         NOT NULL DEFAULT TRUE,
    version             INT             NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          BIGINT,
    trace_id            VARCHAR(128),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (scene_code),
    CONSTRAINT chk_scene_model_type CHECK (model_type IN ('CHAT', 'EMBEDDING')),
    CONSTRAINT chk_scene_temperature
        CHECK (temperature IS NULL OR (temperature >= 0 AND temperature <= 2)),
    CONSTRAINT chk_scene_top_p
        CHECK (top_p IS NULL OR (top_p > 0 AND top_p <= 1)),
    CONSTRAINT chk_scene_max_input_tokens
        CHECK (max_input_tokens BETWEEN 256 AND 1000000),
    CONSTRAINT chk_scene_max_output_tokens
        CHECK (max_output_tokens BETWEEN 1 AND 32768),
    CONSTRAINT chk_scene_timeout CHECK (timeout_seconds BETWEEN 5 AND 180),
    CONSTRAINT chk_scene_version CHECK (version >= 0),
    CONSTRAINT chk_scene_extra_options
        CHECK (jsonb_typeof(extra_options) = 'object')
);

CREATE INDEX IF NOT EXISTS idx_llm_scene_provider
    ON llm_scene_config (provider_id);
CREATE INDEX IF NOT EXISTS idx_llm_scene_type_enabled
    ON llm_scene_config (model_type, enabled);

COMMENT ON TABLE llm_scene_config IS 'AI 场景执行参数表';
COMMENT ON COLUMN llm_scene_config.scene_code IS '代码预定义的 AI 业务场景编码';
COMMENT ON COLUMN llm_scene_config.model_type IS 'CHAT / EMBEDDING';
COMMENT ON COLUMN llm_scene_config.provider_id IS
    '[逻辑外键]→llm_provider_config；为空时使用对应全局默认 Provider';
COMMENT ON COLUMN llm_scene_config.extra_options IS
    '仅允许服务端白名单中的供应商特有参数';
COMMENT ON COLUMN llm_scene_config.version IS '管理端 CAS 版本';

-- 第二阶段只初始化简历分析和人岗匹配场景。
INSERT INTO llm_scene_config (
    scene_code,
    model_type,
    provider_id,
    temperature,
    top_p,
    max_input_tokens,
    max_output_tokens,
    timeout_seconds,
    prompt_version,
    extra_options,
    enabled,
    version,
    created_at,
    updated_at
) VALUES
(
    'RESUME_ANALYSIS', 'CHAT', NULL, 0.200, 0.900,
    16000, 2000, 60, 'v1', '{}'::jsonb,
    TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    'CANDIDATE_PROFILE_GENERATION', 'CHAT', NULL, 0.200, 0.900,
    16000, 2000, 60, 'v1', '{}'::jsonb,
    TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    'HR_APPLICATION_SCREENING', 'CHAT', NULL, 0.200, 0.900,
    16000, 1000, 60, 'v1', '{}'::jsonb,
    TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    'CANDIDATE_JOB_MATCHING', 'CHAT', NULL, 0.200, 0.900,
    16000, 1000, 60, 'v1', '{}'::jsonb,
    TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
ON CONFLICT (scene_code) DO NOTHING;

COMMIT;
