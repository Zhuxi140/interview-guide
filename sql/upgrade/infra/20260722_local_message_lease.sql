-- local_message 显式租约与未来 MQ Outbox 协议字段。
ALTER TABLE local_message
    ADD COLUMN IF NOT EXISTS biz_key VARCHAR(160),
    ADD COLUMN IF NOT EXISTS schema_version SMALLINT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS lease_owner VARCHAR(128),
    ADD COLUMN IF NOT EXISTS lease_until TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS lease_version BIGINT NOT NULL DEFAULT 0;

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

-- 回滚提示：确认没有新版本应用运行后，先删除以上索引，再删除新增字段。
