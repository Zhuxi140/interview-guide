BEGIN;

-- 面试编排草案增加 LLM 配置快照列：固化每次生成实际使用的场景/Provider/模型/参数及版本，不含密钥。
ALTER TABLE interview_plan_drafts
    ADD COLUMN IF NOT EXISTS llm_config_snapshot JSONB;

ALTER TABLE interview_plan_drafts DROP CONSTRAINT IF EXISTS ck_interview_plan_llm_snapshot_object;
ALTER TABLE interview_plan_drafts
    ADD CONSTRAINT ck_interview_plan_llm_snapshot_object
    CHECK (llm_config_snapshot IS NULL OR jsonb_typeof(llm_config_snapshot) = 'object');

COMMENT ON COLUMN interview_plan_drafts.llm_config_snapshot
    IS '本次 Agent 生成实际使用的 LLM 场景、Provider、模型、参数及版本快照，不含密钥';

COMMIT;