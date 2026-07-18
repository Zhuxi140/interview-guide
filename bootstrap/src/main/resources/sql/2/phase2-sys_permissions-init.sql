-- =============================================
-- Phase 2: sys_permissions + sys_role_permissions 初始化
-- =============================================
-- 注意：sys_role_permissions 无 id 字段，主键为 (role_id, permission_id)
-- Permission ID 范围：401-425（401-406: 简历, 411-415: 投递, 421-425: 本地消息运维）

-- ===== sys_permissions =====

INSERT INTO sys_permissions (id, perm_code, perm_type, api_path, status) VALUES
-- 2.1 简历管理
(401, 'resume:upload',         'API', '/api/v1/enterprises/*/resumes', 1),
(402, 'resume:list',           'API', '/api/v1/enterprises/*/resumes', 1),
(403, 'resume:detail',         'API', '/api/v1/enterprises/*/resumes/*', 1),
(404, 'resume:delete',         'API', '/api/v1/enterprises/*/resumes/*', 1),
(405, 'resume:analyze',        'API', '/api/v1/enterprises/*/resumes/*/analyze', 1),
(406, 'resume:analysis-result','API', '/api/v1/enterprises/*/resumes/*/analysis', 1),

-- 2.2 投递与初筛
(411, 'application:apply',           'API', '/api/v1/jobs/*/apply', 1),
(412, 'application:list',            'API', '/api/v1/enterprises/*/jobs/*/applications', 1),
(413, 'application:detail',          'API', '/api/v1/enterprises/*/applications/*', 1),
(414, 'application:update-status',   'API', '/api/v1/enterprises/*/applications/*/status', 1),
(415, 'candidate:applications',      'API', '/api/v1/candidate/applications', 1);

-- ===== sys_role_permissions =====

-- SUPER_ADMIN (1001) — 全部 11 个权限
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(1001, 401, NOW()), (1001, 402, NOW()), (1001, 403, NOW()), (1001, 404, NOW()), (1001, 405, NOW()), (1001, 406, NOW()),
(1001, 411, NOW()), (1001, 412, NOW()), (1001, 413, NOW()), (1001, 414, NOW()), (1001, 415, NOW());

-- FINANCE_ADMIN (1002) — 仅查看简历
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(1002, 402, NOW()), (1002, 403, NOW());

-- ENTERPRISE_OWNER (2001) — 全部企业级权限
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2001, 401, NOW()), (2001, 402, NOW()), (2001, 403, NOW()), (2001, 404, NOW()), (2001, 405, NOW()), (2001, 406, NOW()),
(2001, 412, NOW()), (2001, 413, NOW()), (2001, 414, NOW());

-- ENTERPRISE_ADMIN (2002) — 与企业所有者一致
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2002, 401, NOW()), (2002, 402, NOW()), (2002, 403, NOW()), (2002, 404, NOW()), (2002, 405, NOW()), (2002, 406, NOW()),
(2002, 412, NOW()), (2002, 413, NOW()), (2002, 414, NOW());

-- HR_MANAGER (2003) — 简历全部 + 投递管理全部
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2003, 401, NOW()), (2003, 402, NOW()), (2003, 403, NOW()), (2003, 404, NOW()), (2003, 405, NOW()), (2003, 406, NOW()),
(2003, 412, NOW()), (2003, 413, NOW()), (2003, 414, NOW());

-- HR_RECRUITER (2004) — 简历除删除外 + 投递管理（无删除岗位权限同理）
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2004, 401, NOW()), (2004, 402, NOW()), (2004, 403, NOW()), (2004, 405, NOW()), (2004, 406, NOW()),
(2004, 412, NOW()), (2004, 413, NOW()), (2004, 414, NOW());

-- INTERVIEWER (2005) — 仅查看简历和投递
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2005, 402, NOW()), (2005, 403, NOW()), (2005, 406, NOW()),
(2005, 412, NOW()), (2005, 413, NOW());

-- CANDIDATE (3001) — 投递简历 + 查看我的投递
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(3001, 411, NOW()), (3001, 415, NOW());

-- =============================================
-- 2.5 本地消息管理 ops:local-message:* 权限
-- =============================================

INSERT INTO sys_permissions (id, perm_code, perm_type, api_path, status) VALUES
(421, 'ops:local-message:page',        'API', '/api/v1/admin/local-messages/page', 1),
(422, 'ops:local-message:detail',      'API', '/api/v1/admin/local-messages/*', 1),
(423, 'ops:local-message:retry',       'API', '/api/v1/admin/local-messages/*/retry', 1),
(424, 'ops:local-message:batch-retry', 'API', '/api/v1/admin/local-messages/batch-retry', 1),
(425, 'ops:local-message:status',      'API', '/api/v1/admin/local-messages/*/status', 1);

-- SUPER_ADMIN (1001) — 追加 5 条 ops:* 权限
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(1001, 421, NOW()), (1001, 422, NOW()), (1001, 423, NOW()), (1001, 424, NOW()), (1001, 425, NOW());

-- PLATFORM_OPS (1003) — 仅 5 条 ops:* 权限
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(1003, 421, NOW()), (1003, 422, NOW()), (1003, 423, NOW()), (1003, 424, NOW()), (1003, 425, NOW());
