-- =============================================
-- Phase 1: sys_permissions + sys_role_permissions 初始化
-- =============================================
-- 注意：sys_role_permissions 无 id 字段，主键为 (role_id, permission_id)

-- ===== sys_permissions =====

INSERT INTO sys_permissions (id, perm_code, perm_type, api_path, status) VALUES
-- 1.2 企业管理
(101, 'enterprise:create', 'API', '/api/v1/enterprises', 1),
(102, 'enterprise:list',   'API', '/api/v1/enterprises', 1),
(103, 'enterprise:detail', 'API', '/api/v1/enterprises/*', 1),
(104, 'enterprise:update', 'API', '/api/v1/enterprises/*', 1),
(105, 'enterprise:delete', 'API', '/api/v1/enterprises/*', 1),
(106, 'enterprise:update-contact', 'API', '/api/v1/enterprises/*/contact*', 1),

-- 1.2 团队成员管理
(111, 'team:list',   'API', '/api/v1/enterprises/*/members', 1),
(112, 'team:invite', 'API', '/api/v1/enterprises/*/members', 1),
(113, 'team:update', 'API', '/api/v1/enterprises/*/members/*', 1),
(114, 'team:remove', 'API', '/api/v1/enterprises/*/members/*', 1),

-- 1.3 角色查阅 (Admin) — 角色由 SQL 预置，仅查询
(203, 'admin:roles:list',   'API', '/api/v1/admin/roles', 1),
(204, 'admin:roles:detail', 'API', '/api/v1/admin/roles/*', 1),

-- 1.3 用户角色关联 (Admin)
(221, 'admin:user-roles:assign', 'API', '/api/v1/admin/users/*/roles', 1),
(222, 'admin:user-roles:list',   'API', '/api/v1/admin/users/*/roles', 1),
(223, 'admin:user-roles:remove', 'API', '/api/v1/admin/users/*/roles', 1),

-- 1.4 岗位管理
(301, 'job:create', 'API', '/api/v1/enterprises/*/jobs', 1),
(302, 'job:list',   'API', '/api/v1/enterprises/*/jobs', 1),
(303, 'job:detail', 'API', '/api/v1/enterprises/*/jobs/*', 1),
(304, 'job:update', 'API', '/api/v1/enterprises/*/jobs/*', 1),
(305, 'job:toggle-status', 'API', '/api/v1/enterprises/*/jobs/*/status', 1),
(306, 'job:delete', 'API', '/api/v1/enterprises/*/jobs/*', 1)
ON CONFLICT (id) DO UPDATE SET
    perm_code = EXCLUDED.perm_code,
    perm_type = EXCLUDED.perm_type,
    api_path = EXCLUDED.api_path,
    status = EXCLUDED.status;

-- ===== sys_role_permissions =====

-- SUPER_ADMIN (1001) — 全部 21 个权限
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(1001, 101, NOW()), (1001, 102, NOW()), (1001, 103, NOW()), (1001, 104, NOW()), (1001, 105, NOW()), (1001, 106, NOW()),
(1001, 111, NOW()), (1001, 112, NOW()), (1001, 113, NOW()), (1001, 114, NOW()),
(1001, 203, NOW()), (1001, 204, NOW()),
(1001, 221, NOW()), (1001, 222, NOW()), (1001, 223, NOW()),
(1001, 301, NOW()), (1001, 302, NOW()), (1001, 303, NOW()), (1001, 304, NOW()), (1001, 305, NOW()), (1001, 306, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- FINANCE_ADMIN (1002) — Phase 1 仅企业查看，Phase 4 计费上线后补充
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(1002, 102, NOW()), (1002, 103, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ENTERPRISE_OWNER (2001) — 企业/团队/岗位全部（无 Admin）
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2001, 101, NOW()), (2001, 102, NOW()), (2001, 103, NOW()), (2001, 104, NOW()), (2001, 105, NOW()), (2001, 106, NOW()),
(2001, 111, NOW()), (2001, 112, NOW()), (2001, 113, NOW()), (2001, 114, NOW()),
(2001, 301, NOW()), (2001, 302, NOW()), (2001, 303, NOW()), (2001, 304, NOW()), (2001, 305, NOW()), (2001, 306, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ENTERPRISE_ADMIN (2002) — 与 OWNER 一致，仅去掉 enterprise:delete
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2002, 101, NOW()), (2002, 102, NOW()), (2002, 103, NOW()), (2002, 104, NOW()), (2002, 106, NOW()),
(2002, 111, NOW()), (2002, 112, NOW()), (2002, 113, NOW()), (2002, 114, NOW()),
(2002, 301, NOW()), (2002, 302, NOW()), (2002, 303, NOW()), (2002, 304, NOW()), (2002, 305, NOW()), (2002, 306, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- HR_MANAGER (2003) — 团队查看/邀请/改角色 + 岗位全部
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2003, 111, NOW()), (2003, 112, NOW()), (2003, 113, NOW()),
(2003, 301, NOW()), (2003, 302, NOW()), (2003, 303, NOW()), (2003, 304, NOW()), (2003, 305, NOW()), (2003, 306, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- HR_RECRUITER (2004) — 岗位发布/查看/编辑，无删除和开关
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2004, 301, NOW()), (2004, 302, NOW()), (2004, 303, NOW()), (2004, 304, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- INTERVIEWER (2005) — 仅查看岗位
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2005, 302, NOW()), (2005, 303, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- CANDIDATE (3001) — Phase 1 无对应 API 权限，认证接口走 JWT 拦截器


