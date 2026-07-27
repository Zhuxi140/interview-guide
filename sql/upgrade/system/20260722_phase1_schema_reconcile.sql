-- Phase 1 旧库结构收敛；仅操作 system 模块拥有的表。

-- 角色主键使用固定 INTEGER 编码，关联表保持相同类型。
ALTER TABLE sys_user_roles
    ALTER COLUMN role_id TYPE INTEGER USING role_id::INTEGER;
ALTER TABLE sys_role_permissions
    ALTER COLUMN role_id TYPE INTEGER USING role_id::INTEGER;

-- 联合主键已经保证唯一，删除历史重复约束和索引。
ALTER TABLE sys_user_roles DROP CONSTRAINT IF EXISTS sys_user_roles_pk;
DROP INDEX IF EXISTS idx_user_role;
DROP INDEX IF EXISTS idx_role_permission;

-- 统一调用链字段长度。
ALTER TABLE sys_roles ALTER COLUMN trace_id TYPE VARCHAR(128);
ALTER TABLE sys_permissions ALTER COLUMN trace_id TYPE VARCHAR(128);
ALTER TABLE sys_role_permissions ALTER COLUMN trace_id TYPE VARCHAR(128);
ALTER TABLE enterprises ALTER COLUMN trace_id TYPE VARCHAR(128);
ALTER TABLE enterprise_team_members ALTER COLUMN trace_id TYPE VARCHAR(128);

-- 企业默认进入待认证状态，并修复旧库约束与软删除唯一索引。
UPDATE enterprises SET short_name = name WHERE short_name IS NULL;
UPDATE enterprises SET status = 2 WHERE status IS NULL;
ALTER TABLE enterprises
    ALTER COLUMN short_name SET NOT NULL,
    ALTER COLUMN status SET DEFAULT 2,
    ALTER COLUMN status SET NOT NULL;
ALTER TABLE enterprises DROP CONSTRAINT IF EXISTS enterprises_pk;
CREATE UNIQUE INDEX IF NOT EXISTS idx_enterprises_name_active
    ON enterprises (name) WHERE is_deleted = FALSE;

-- role_id 已成为唯一角色来源；旧时间字段按历史 UTC 语义转换。
ALTER TABLE enterprise_team_members DROP COLUMN IF EXISTS member_role;
ALTER TABLE enterprise_team_members
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ
    USING updated_at AT TIME ZONE 'UTC';

-- 删除已下线的任意消息状态修改权限，并同步现有接口路径。
DELETE FROM sys_role_permissions WHERE permission_id = 425;
DELETE FROM sys_permissions
WHERE id = 425 OR perm_code = 'ops:local-message:status';
UPDATE sys_permissions
SET api_path = '/api/v1/enterprises/*/contact*'
WHERE id = 106;
UPDATE sys_permissions
SET api_path = '/api/v1/candidate/jobs/*/apply'
WHERE id = 411;

-- 过期令牌保留审计记录，但不再作为有效令牌参与查询。
UPDATE user_tokens
SET is_revoked = TRUE,
    is_deleted = TRUE
WHERE expires_at < CURRENT_TIMESTAMP
  AND (is_revoked = FALSE OR is_deleted = FALSE);
