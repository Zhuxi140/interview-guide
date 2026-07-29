-- ============================================================
-- BPortal：为简历 AI 分析结果增加不可变 LLM 配置快照
-- PostgreSQL 14+
-- ============================================================

BEGIN;

ALTER TABLE resume_analyses
    ADD COLUMN IF NOT EXISTS llm_config_snapshot JSONB;

-- 历史结果无法还原当时的实际模型参数，仅标记为旧数据。
UPDATE resume_analyses
SET llm_config_snapshot = '{"legacy":true}'::jsonb
WHERE llm_config_snapshot IS NULL;

ALTER TABLE resume_analyses
    ALTER COLUMN llm_config_snapshot SET NOT NULL,
    ALTER COLUMN analyzed_at DROP NOT NULL,
    ALTER COLUMN is_deleted SET DEFAULT FALSE,
    ALTER COLUMN is_deleted SET NOT NULL,
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'resume_analyses'::regclass
          AND conname = 'chk_resume_analysis_llm_snapshot'
    ) THEN
        ALTER TABLE resume_analyses
            ADD CONSTRAINT chk_resume_analysis_llm_snapshot
            CHECK (jsonb_typeof(llm_config_snapshot) = 'object');
    END IF;
END
$$;

COMMENT ON COLUMN resume_analyses.llm_config_snapshot IS
    '任务创建时固化的 LLM 场景、Provider、模型、参数及版本快照，不含密钥';
COMMENT ON COLUMN resume_analyses.analyzed_at IS
    '分析完成时间；任务待执行或处理中为空';

COMMIT;
