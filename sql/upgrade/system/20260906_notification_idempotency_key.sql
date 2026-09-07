-- sys_notifications 增加幂等键列与部分唯一索引。
-- 用途：站内信同步发送的防重（业务重试 / 并发触发不产生重复通知）。
-- 幂等键由调用方生成，建议格式 {scene}:{bizId}，例如 OFFER_SENT:12345；
-- 投递流水（EMAIL_LOG / SMS_LOG）不带该键，可多行并存。
-- 部分唯一索引只约束非空且未删除的行，逻辑删除后允许同键重新发送。

ALTER TABLE sys_notifications
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS uk_notifications_idem
    ON sys_notifications (idempotency_key)
    WHERE idempotency_key IS NOT NULL AND is_deleted = FALSE;

COMMENT ON COLUMN sys_notifications.idempotency_key IS
    '发送幂等键（{scene}:{bizId}）；唯一索引仅约束非空且未删除的行';
