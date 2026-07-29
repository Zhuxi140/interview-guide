-- AI 默认路由从单例宽表迁移为按模型类型分行，并为 Provider 补充能力类型。
-- 执行前提：已执行 20260728_llm_scene_config.sql。

BEGIN;

ALTER TABLE llm_provider_config
    ADD COLUMN IF NOT EXISTS model_type VARCHAR(32);

-- 旧结构只明确记录 CHAT / EMBEDDING 引用，先从这些确定关系回填类型。
UPDATE llm_provider_config provider
SET model_type = 'CHAT'
FROM llm_global_setting setting
WHERE provider.id = setting.default_chat_provider_id
  AND provider.model_type IS NULL;

UPDATE llm_provider_config provider
SET model_type = 'EMBEDDING'
FROM llm_global_setting setting
WHERE provider.id = setting.default_embedding_provider_id
  AND provider.model_type IS NULL;

-- 禁止猜测未被旧全局设置引用的 Provider 类型。
DO
$$
DECLARE
    unresolved_ids TEXT;
BEGIN
    SELECT string_agg(id, ', ' ORDER BY id)
    INTO unresolved_ids
    FROM llm_provider_config
    WHERE model_type IS NULL;

    IF unresolved_ids IS NOT NULL THEN
        RAISE EXCEPTION
            '无法自动确定以下 Provider 的 model_type，请先人工补齐后重试：%',
            unresolved_ids;
    END IF;
END
$$;

ALTER TABLE llm_provider_config
    ALTER COLUMN model_type SET NOT NULL;

ALTER TABLE llm_provider_config
    DROP CONSTRAINT IF EXISTS chk_llm_provider_model_type;

ALTER TABLE llm_provider_config
    ADD CONSTRAINT chk_llm_provider_model_type
        CHECK (model_type IN ('CHAT', 'EMBEDDING', 'ASR', 'TTS'));

COMMENT ON COLUMN llm_provider_config.model_type IS
    '模型能力类型：CHAT / EMBEDDING / ASR / TTS';

CREATE TABLE ai_global_route (
    model_type     VARCHAR(32)     NOT NULL,
    provider_id    VARCHAR(64),
    version        INT             NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by     BIGINT,
    trace_id       VARCHAR(128),
    updated_at     TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (model_type),
    CONSTRAINT chk_ai_global_route_model_type
        CHECK (model_type IN ('CHAT', 'EMBEDDING', 'ASR', 'TTS')),
    CONSTRAINT chk_ai_global_route_version CHECK (version >= 0)
);

INSERT INTO ai_global_route (
    model_type,
    provider_id,
    version,
    created_at,
    updated_by,
    trace_id,
    updated_at
)
SELECT
    route.model_type,
    route.provider_id,
    setting.version,
    setting.created_at,
    setting.updated_by,
    setting.trace_id,
    setting.updated_at
FROM llm_global_setting setting
CROSS JOIN LATERAL (
    VALUES
        ('CHAT', setting.default_chat_provider_id),
        ('EMBEDDING', setting.default_embedding_provider_id),
        ('ASR', NULL::VARCHAR),
        ('TTS', NULL::VARCHAR)
) AS route(model_type, provider_id);

COMMENT ON TABLE ai_global_route IS '按模型能力划分的 AI 全局默认路由表';
COMMENT ON COLUMN ai_global_route.model_type IS
    '模型能力类型，每种类型只有一个默认路由槽位';
COMMENT ON COLUMN ai_global_route.provider_id IS
    '[逻辑外键]→llm_provider_config；为空表示尚未配置';
COMMENT ON COLUMN ai_global_route.version IS
    '该模型类型默认路由的管理端 CAS 版本';
COMMENT ON COLUMN ai_global_route.created_at IS '创建时间';
COMMENT ON COLUMN ai_global_route.updated_by IS '[逻辑外键]→sys_users，最后修改人';
COMMENT ON COLUMN ai_global_route.trace_id IS '最后一次修改的调用链 ID';
COMMENT ON COLUMN ai_global_route.updated_at IS '最后更新时间';

COMMENT ON COLUMN llm_scene_config.provider_id IS
    '[逻辑外键]→llm_provider_config；为空时按 model_type 使用 ai_global_route';

DROP TABLE llm_global_setting;

COMMIT;
