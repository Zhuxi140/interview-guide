-- =============================================
-- Phase 8: sys_permissions + sys_role_permissions 初始化（风控与合规增强）
-- =============================================
-- 注意：sys_role_permissions 无 id 字段，主键为 (role_id, permission_id)

-- ===== sys_permissions =====

INSERT INTO sys_permissions (id, perm_code, perm_type, api_path, status) VALUES
-- 8.1 个人实名认证 (KYC)
(1001, 'kyc:submit', 'API', '/api/v1/candidate/kyc', 1),
(1002, 'kyc:status', 'API', '/api/v1/candidate/kyc/status', 1),

-- 8.1 企业资质认证
(1005, 'enterprise:cert:submit', 'API', '/api/v1/enterprises/*/certification', 1),
(1006, 'enterprise:cert:status', 'API', '/api/v1/enterprises/*/certification/status', 1),

-- 8.1 Admin KYC 审核
(1003, 'admin:kyc:audit', 'API', '/api/v1/admin/kyc/*/audit', 1),
(1004, 'admin:kyc:list',   'API', '/api/v1/admin/kyc', 1),

-- 8.1 Admin 企业认证审核
(1007, 'admin:cert:audit', 'API', '/api/v1/admin/cert/*/audit', 1),
(1008, 'admin:cert:list',   'API', '/api/v1/admin/cert', 1),

-- 8.2 敏感词管理
(1009, 'admin:sensitive-words:list',   'API', '/api/v1/admin/sensitive-words', 1),
(1010, 'admin:sensitive-words:create', 'API', '/api/v1/admin/sensitive-words', 1),
(1011, 'admin:sensitive-words:update', 'API', '/api/v1/admin/sensitive-words/*', 1),
(1012, 'admin:sensitive-words:delete', 'API', '/api/v1/admin/sensitive-words/*', 1),

-- 8.3 防作弊日志
(1013, 'admin:anti-cheat:list',   'API', '/api/v1/admin/anti-cheat-logs', 1),
(1014, 'admin:anti-cheat:detail', 'API', '/api/v1/admin/anti-cheat-logs/*', 1),

-- 8.4 API 策略管理
(1015, 'admin:api-policies:create', 'API', '/api/v1/admin/api-policies', 1),
(1016, 'admin:api-policies:list',   'API', '/api/v1/admin/api-policies', 1),
(1017, 'admin:api-policies:update', 'API', '/api/v1/admin/api-policies/*', 1),
(1018, 'admin:api-policies:delete', 'API', '/api/v1/admin/api-policies/*', 1)
ON CONFLICT (id) DO UPDATE SET
    perm_code = EXCLUDED.perm_code,
    perm_type = EXCLUDED.perm_type,
    api_path = EXCLUDED.api_path,
    status = EXCLUDED.status;

-- ===== sys_role_permissions =====

-- SUPER_ADMIN (1001) — 全部 Phase 8 权限
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(1001, 1001, NOW()), (1001, 1002, NOW()), (1001, 1003, NOW()), (1001, 1004, NOW()),
(1001, 1005, NOW()), (1001, 1006, NOW()), (1001, 1007, NOW()), (1001, 1008, NOW()),
(1001, 1009, NOW()), (1001, 1010, NOW()), (1001, 1011, NOW()), (1001, 1012, NOW()),
(1001, 1013, NOW()), (1001, 1014, NOW()),
(1001, 1015, NOW()), (1001, 1016, NOW()), (1001, 1017, NOW()), (1001, 1018, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ENTERPRISE_OWNER (2001) — 企业认证提交/查询
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2001, 1005, NOW()), (2001, 1006, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ENTERPRISE_ADMIN (2002) — 企业认证提交/查询
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2002, 1005, NOW()), (2002, 1006, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- CANDIDATE (3001) — 个人实名认证提交/查询
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(3001, 1001, NOW()), (3001, 1002, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;
