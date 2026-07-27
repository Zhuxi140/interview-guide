-- Phase 1/2 业务表旧库结构收敛；仅操作 bportal 模块拥有的表。

-- 岗位枚举持久化为数值编码，并补充管理端乐观锁。
ALTER TABLE jobs
    ALTER COLUMN experience_req TYPE SMALLINT USING experience_req::SMALLINT,
    ALTER COLUMN education_req TYPE SMALLINT USING education_req::SMALLINT,
    ALTER COLUMN status SET DEFAULT 2,
    ALTER COLUMN trace_id TYPE VARCHAR(128),
    ADD COLUMN IF NOT EXISTS version INT NOT NULL DEFAULT 0;
CREATE INDEX IF NOT EXISTS idx_jobs_enterprise_status
    ON jobs (enterprise_id, status);

COMMENT ON COLUMN jobs.experience_req IS '经验要求枚举编码';
COMMENT ON COLUMN jobs.education_req IS '学历要求枚举编码';
COMMENT ON COLUMN jobs.status IS '岗位状态 (2: 草稿, 1: 开放中, 0: 已关闭)';
COMMENT ON COLUMN jobs.version IS '管理端编辑使用的乐观锁版本号';

-- 投递使用业务期望状态做原子更新，并通过候选人维度的幂等键防止请求重放。
ALTER TABLE job_applications
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(128);

COMMENT ON COLUMN job_applications.idempotency_key IS '候选人投递请求幂等键';
COMMENT ON COLUMN job_applications.status IS 'APPLIED / REVIEWING / PASSED / REJECTED / WITHDRAWN';

CREATE UNIQUE INDEX IF NOT EXISTS uk_applications_candidate_idempotency_active
    ON job_applications (candidate_id, idempotency_key)
    WHERE is_deleted = false AND idempotency_key IS NOT NULL;
