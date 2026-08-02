-- 将旧企业侧账号类型 HR 迁移为 ENTERPRISE_USER。
UPDATE sys_users
SET user_type = 'ENTERPRISE_USER',
    updated_at = CURRENT_TIMESTAMP
WHERE user_type = 'HR';

COMMENT ON COLUMN sys_users.user_type IS
    '用户类型 (PLATFORM_ADMIN / PLATFORM_OPS / ENTERPRISE_USER / CANDIDATE)';
