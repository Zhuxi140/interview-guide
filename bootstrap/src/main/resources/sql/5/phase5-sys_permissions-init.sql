-- 第五阶段：语音面试权限初始化
-- 统一会话的 join-token、detail、end 权限由第三阶段管理；
-- 本阶段仅新增语音消息和语音评估两个资源权限。
-- sys_role_permissions 主键为 (role_id, permission_id)，无 id 字段。

BEGIN;

-- 清理旧版独立 /voice-sessions 状态机权限及角色绑定。
DELETE FROM sys_role_permissions
WHERE permission_id IN (
    SELECT id
    FROM sys_permissions
    WHERE id IN (701, 702, 703)
       OR perm_code IN (
           'voice:session:start',
           'voice:session:status',
           'voice:session:end'
       )
);

DELETE FROM sys_permissions
WHERE id IN (701, 702, 703)
   OR perm_code IN (
       'voice:session:start',
       'voice:session:status',
       'voice:session:end'
   );

-- 第五阶段语音专属只读资源。
INSERT INTO sys_permissions (id, perm_code, perm_type, api_path, status) VALUES
(704, 'voice:messages:list',
 'API', '/api/v1/interview-sessions/*/voice/messages', 1),
(705, 'voice:evaluation:get',
 'API', '/api/v1/interview-sessions/*/voice/evaluation', 1)
ON CONFLICT (id) DO UPDATE SET
    perm_code = EXCLUDED.perm_code,
    perm_type = EXCLUDED.perm_type,
    api_path = EXCLUDED.api_path,
    status = EXCLUDED.status;

-- 平台超级管理员：排障和审计。
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(1001, 704, NOW()),
(1001, 705, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 企业所有者、管理员、HR 和面试官：仍需在接口层校验企业归属。
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2001, 704, NOW()), (2001, 705, NOW()),
(2002, 704, NOW()), (2002, 705, NOW()),
(2003, 704, NOW()), (2003, 705, NOW()),
(2004, 704, NOW()), (2004, 705, NOW()),
(2005, 704, NOW()), (2005, 705, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 候选人：仍需在接口层校验 session.user_id 为当前用户。
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(3001, 704, NOW()),
(3001, 705, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

COMMIT;
