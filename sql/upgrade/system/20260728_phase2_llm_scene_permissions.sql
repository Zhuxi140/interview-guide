-- ============================================================
-- System：第二阶段 LLM 场景配置权限
-- ============================================================

BEGIN;

INSERT INTO sys_permissions (id, perm_code, perm_type, api_path, status) VALUES
(440, 'admin:llm:scene:list',
 'API', '/api/v1/admin/llm/scenes', 1),
(441, 'admin:llm:scene:detail',
 'API', '/api/v1/admin/llm/scenes/*', 1),
(442, 'admin:llm:scene:update',
 'API', '/api/v1/admin/llm/scenes/*', 1),
(443, 'admin:llm:scene:status',
 'API', '/api/v1/admin/llm/scenes/*/status', 1)
ON CONFLICT (id) DO UPDATE SET
    perm_code = EXCLUDED.perm_code,
    perm_type = EXCLUDED.perm_type,
    api_path = EXCLUDED.api_path,
    status = EXCLUDED.status;

-- 超级管理员可维护场景配置。
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(1001, 440, CURRENT_TIMESTAMP),
(1001, 441, CURRENT_TIMESTAMP),
(1001, 442, CURRENT_TIMESTAMP),
(1001, 443, CURRENT_TIMESTAMP)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 平台运维仅可查看场景列表和详情。
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(1003, 440, CURRENT_TIMESTAMP),
(1003, 441, CURRENT_TIMESTAMP)
ON CONFLICT (role_id, permission_id) DO NOTHING;

COMMIT;
