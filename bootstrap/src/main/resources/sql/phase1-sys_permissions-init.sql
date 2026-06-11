-- =============================================
-- Phase 1: sys_permissions 初始化数据
-- ID 显式指定，便于 sys_role_permissions 引用
-- =============================================

-- 1.2 企业管理
INSERT INTO sys_permissions (id, perm_code, perm_type, api_path, status) VALUES
(101, 'enterprise:create', 'API', '/api/v1/enterprises', 1),
(102, 'enterprise:list',   'API', '/api/v1/enterprises', 1),
(103, 'enterprise:detail', 'API', '/api/v1/enterprises/*', 1),
(104, 'enterprise:update', 'API', '/api/v1/enterprises/*', 1),
(105, 'enterprise:delete', 'API', '/api/v1/enterprises/*', 1);

-- 1.2 团队成员管理
INSERT INTO sys_permissions (id, perm_code, perm_type, api_path, status) VALUES
(111, 'team:list',   'API', '/api/v1/enterprises/*/members', 1),
(112, 'team:invite', 'API', '/api/v1/enterprises/*/members', 1),
(113, 'team:update', 'API', '/api/v1/enterprises/*/members/*', 1),
(114, 'team:remove', 'API', '/api/v1/enterprises/*/members/*', 1);

-- 1.3 角色管理 (Admin)
INSERT INTO sys_permissions (id, perm_code, perm_type, api_path, status) VALUES
(201, 'admin:roles:create', 'API', '/api/v1/admin/roles', 1),
(202, 'admin:roles:delete', 'API', '/api/v1/admin/roles/*', 1),
(203, 'admin:roles:list',   'API', '/api/v1/admin/roles', 1),
(204, 'admin:roles:detail', 'API', '/api/v1/admin/roles/*', 1),
(205, 'admin:roles:update', 'API', '/api/v1/admin/roles/*', 1);

-- 1.3 权限管理 (Admin)
INSERT INTO sys_permissions (id, perm_code, perm_type, api_path, status) VALUES
(211, 'admin:permissions:list',        'API', '/api/v1/admin/permissions', 1),
(212, 'admin:permissions:assign',      'API', '/api/v1/admin/roles/*/permissions', 1),
(213, 'admin:permissions:get-by-role', 'API', '/api/v1/admin/roles/*/permissions', 1),
(214, 'admin:permissions:remove',      'API', '/api/v1/admin/roles/*/permissions', 1);

-- 1.3 用户角色关联 (Admin)
INSERT INTO sys_permissions (id, perm_code, perm_type, api_path, status) VALUES
(221, 'admin:user-roles:assign', 'API', '/api/v1/admin/users/*/roles', 1),
(222, 'admin:user-roles:list',   'API', '/api/v1/admin/users/*/roles', 1),
(223, 'admin:user-roles:remove', 'API', '/api/v1/admin/users/*/roles', 1);

-- 1.4 岗位管理
INSERT INTO sys_permissions (id, perm_code, perm_type, api_path, status) VALUES
(301, 'job:create', 'API', '/api/v1/enterprises/*/jobs', 1),
(302, 'job:list',   'API', '/api/v1/enterprises/*/jobs', 1),
(303, 'job:detail', 'API', '/api/v1/enterprises/*/jobs/*', 1),
(304, 'job:update', 'API', '/api/v1/enterprises/*/jobs/*', 1),
(305, 'job:toggle-status', 'API', '/api/v1/enterprises/*/jobs/*/status', 1),
(306, 'job:delete', 'API', '/api/v1/enterprises/*/jobs/*', 1);

-- ===== sys_role_permissions =====

-- 共 73 条记录，ID 从 1 开始顺序编号

-- SUPER_ADMIN (1001) — 全部 27 个权限
INSERT INTO sys_role_permissions (id, role_id, permission_id, created_at) VALUES
                                                                              (1, 1001, 101, NOW()), (2, 1001, 102, NOW()), (3, 1001, 103, NOW()), (4, 1001, 104, NOW()), (5, 1001, 105, NOW()),
                                                                              (6, 1001, 111, NOW()), (7, 1001, 112, NOW()), (8, 1001, 113, NOW()), (9, 1001, 114, NOW()),
                                                                              (10, 1001, 201, NOW()), (11, 1001, 202, NOW()), (12, 1001, 203, NOW()), (13, 1001, 204, NOW()), (14, 1001, 205, NOW()),
                                                                              (15, 1001, 211, NOW()), (16, 1001, 212, NOW()), (17, 1001, 213, NOW()), (18, 1001, 214, NOW()),
                                                                              (19, 1001, 221, NOW()), (20, 1001, 222, NOW()), (21, 1001, 223, NOW()),
                                                                              (22, 1001, 301, NOW()), (23, 1001, 302, NOW()), (24, 1001, 303, NOW()), (25, 1001, 304, NOW()), (26, 1001, 305, NOW()), (27, 1001, 306, NOW());

-- FINANCE_ADMIN (1002) — Phase 1 仅企业查看，Phase 4 计费上线后补充
INSERT INTO sys_role_permissions (id, role_id, permission_id, created_at) VALUES
                                                                              (28, 1002, 102, NOW()), (29, 1002, 103, NOW());

-- ENTERPRISE_OWNER (2001) — 企业/团队/岗位全部（无 Admin）
INSERT INTO sys_role_permissions (id, role_id, permission_id, created_at) VALUES
                                                                              (30, 2001, 101, NOW()), (31, 2001, 102, NOW()), (32, 2001, 103, NOW()), (33, 2001, 104, NOW()), (34, 2001, 105, NOW()),
                                                                              (35, 2001, 111, NOW()), (36, 2001, 112, NOW()), (37, 2001, 113, NOW()), (38, 2001, 114, NOW()),
                                                                              (39, 2001, 301, NOW()), (40, 2001, 302, NOW()), (41, 2001, 303, NOW()), (42, 2001, 304, NOW()), (43, 2001, 305, NOW()), (44, 2001, 306, NOW());

-- ENTERPRISE_ADMIN (2002) — 与 OWNER 一致，仅去掉 enterprise:delete
INSERT INTO sys_role_permissions (id, role_id, permission_id, created_at) VALUES
                                                                              (45, 2002, 101, NOW()), (46, 2002, 102, NOW()), (47, 2002, 103, NOW()), (48, 2002, 104, NOW()),
                                                                              (49, 2002, 111, NOW()), (50, 2002, 112, NOW()), (51, 2002, 113, NOW()), (52, 2002, 114, NOW()),
                                                                              (53, 2002, 301, NOW()), (54, 2002, 302, NOW()), (55, 2002, 303, NOW()), (56, 2002, 304, NOW()), (57, 2002, 305, NOW()), (58, 2002, 306, NOW());

-- HR_MANAGER (2003) — 团队查看/邀请/改角色 + 岗位全部
INSERT INTO sys_role_permissions (id, role_id, permission_id, created_at) VALUES
                                                                              (59, 2003, 111, NOW()), (60, 2003, 112, NOW()), (61, 2003, 113, NOW()),
                                                                              (62, 2003, 301, NOW()), (63, 2003, 302, NOW()), (64, 2003, 303, NOW()), (65, 2003, 304, NOW()), (66, 2003, 305, NOW()), (67, 2003, 306, NOW());

-- HR_RECRUITER (2004) — 岗位发布/查看/编辑，无删除和开关
INSERT INTO sys_role_permissions (id, role_id, permission_id, created_at) VALUES
                                                                              (68, 2004, 301, NOW()), (69, 2004, 302, NOW()), (70, 2004, 303, NOW()), (71, 2004, 304, NOW());

-- INTERVIEWER (2005) — 仅查看岗位
INSERT INTO sys_role_permissions (id, role_id, permission_id, created_at) VALUES
                                                                              (72, 2005, 302, NOW()), (73, 2005, 303, NOW());

-- CANDIDATE (3001) — Phase 1 无对应 API 权限，认证接口走 JWT 拦截器
