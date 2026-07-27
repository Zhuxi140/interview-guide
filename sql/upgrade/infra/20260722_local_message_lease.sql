-- local_message 显式租约与未来 MQ Outbox 协议字段。
ALTER TABLE local_message
    ADD COLUMN IF NOT EXISTS biz_key VARCHAR(160),
    ADD COLUMN IF NOT EXISTS schema_version SMALLINT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS lease_owner VARCHAR(128),
    ADD COLUMN IF NOT EXISTS lease_until TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS lease_version BIGINT NOT NULL DEFAULT 0;

-- 收敛旧库中的空值和字段约束，确保调度器无需处理非法状态。
UPDATE local_message
SET schema_version = COALESCE(schema_version, 1),
    priority = COALESCE(priority, 'MEDIUM'),
    status = COALESCE(status, 'PENDING'),
    retry_count = COALESCE(retry_count, 0),
    max_retries = COALESCE(max_retries, 3),
    lease_version = COALESCE(lease_version, 0);

ALTER TABLE local_message
    ALTER COLUMN schema_version SET DEFAULT 1,
    ALTER COLUMN schema_version SET NOT NULL,
    ALTER COLUMN priority SET DEFAULT 'MEDIUM',
    ALTER COLUMN priority SET NOT NULL,
    ALTER COLUMN status TYPE VARCHAR(20),
    ALTER COLUMN status SET DEFAULT 'PENDING',
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN retry_count SET DEFAULT 0,
    ALTER COLUMN retry_count SET NOT NULL,
    ALTER COLUMN max_retries SET DEFAULT 3,
    ALTER COLUMN max_retries SET NOT NULL,
    ALTER COLUMN lease_version SET DEFAULT 0,
    ALTER COLUMN lease_version SET NOT NULL;

-- 旧 FILE_DELETE 载荷只有 URL，无法执行简历状态复查，保留记录但禁止自动消费。
UPDATE local_message
SET status = 'FAILED',
    last_error = 'legacy FILE_DELETE payload requires manual audit',
    lease_owner = NULL,
    lease_until = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE topic = 'FILE_DELETE'
  AND status IN ('PENDING', 'PROCESSING');

-- 同一业务动作只允许存在一条 Outbox 记录。
CREATE UNIQUE INDEX IF NOT EXISTS uk_local_message_topic_biz_key
    ON local_message (topic, biz_key)
    WHERE biz_key IS NOT NULL;

-- 支撑按优先级领取到期任务和过期租约。
CREATE INDEX IF NOT EXISTS idx_local_message_dispatch
    ON local_message (priority, status, next_retry_at, lease_until, created_at)
    WHERE status IN ('PENDING', 'PROCESSING');

-- 新租约索引已覆盖旧调度索引，避免重复写放大。
DROP INDEX IF EXISTS idx_lmsg_status_retry;
DROP INDEX IF EXISTS idx_lmsg_priority;

-- 回滚提示：确认没有新版本应用运行后，先删除以上索引，再删除新增字段。
