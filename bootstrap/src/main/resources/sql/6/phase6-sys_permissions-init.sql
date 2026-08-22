-- 第六阶段：在线代码沙箱权限初始化
-- 来源：docs/第6阶段/phase6-sys_permissions-init.sql，扩展 code-test-case:update (813)。
-- sys_role_permissions 主键为 (role_id, permission_id)，无 id 字段。
-- 全部使用 ON CONFLICT DO NOTHING，脚本可重复执行。

BEGIN;

-- ===== sys_permissions =====

INSERT INTO sys_permissions (id, perm_code, perm_type, api_path, status) VALUES
-- 6.1 编程题库管理 (Admin / Enterprise 复用同一权限码，作用域由接口 scope 决定)
(801, 'code-question:create', 'API', '/api/v1/admin/code-questions', 1),
(802, 'code-question:list',   'API', '/api/v1/admin/code-questions', 1),
(803, 'code-question:detail', 'API', '/api/v1/admin/code-questions/*', 1),
(804, 'code-question:update', 'API', '/api/v1/admin/code-questions/*', 1),
(805, 'code-question:delete', 'API', '/api/v1/admin/code-questions/*', 1),

-- 6.1 测试用例管理 (Admin / Enterprise)
(811, 'code-test-case:create', 'API', '/api/v1/admin/code-questions/*/test-cases', 1),
(812, 'code-test-case:delete', 'API', '/api/v1/admin/code-questions/*/test-cases/*', 1),
(813, 'code-test-case:update', 'API', '/api/v1/admin/code-questions/*/test-cases/*', 1),

-- 6.2 代码提交与评测
(821, 'code-submission:submit',  'API', '/api/v1/interview-sessions/*/code-submit', 1),
(822, 'code-submission:results', 'API', '/api/v1/interview-sessions/*/code-results', 1)
ON CONFLICT (id) DO NOTHING;

-- ===== sys_role_permissions =====

-- SUPER_ADMIN (1001) — 全部 10 个权限
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(1001, 801, NOW()), (1001, 802, NOW()), (1001, 803, NOW()), (1001, 804, NOW()), (1001, 805, NOW()),
(1001, 811, NOW()), (1001, 812, NOW()), (1001, 813, NOW()),
(1001, 821, NOW()), (1001, 822, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- FINANCE_ADMIN (1002) — 无代码沙箱权限

-- ENTERPRISE_OWNER (2001) — 编程题库全部 + 提交与结果
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2001, 801, NOW()), (2001, 802, NOW()), (2001, 803, NOW()), (2001, 804, NOW()), (2001, 805, NOW()),
(2001, 811, NOW()), (2001, 812, NOW()), (2001, 813, NOW()),
(2001, 821, NOW()), (2001, 822, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ENTERPRISE_ADMIN (2002) — 与 OWNER 一致
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2002, 801, NOW()), (2002, 802, NOW()), (2002, 803, NOW()), (2002, 804, NOW()), (2002, 805, NOW()),
(2002, 811, NOW()), (2002, 812, NOW()), (2002, 813, NOW()),
(2002, 821, NOW()), (2002, 822, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- HR_MANAGER (2003) — 查看题库 + 提交与结果
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2003, 802, NOW()), (2003, 803, NOW()),
(2003, 821, NOW()), (2003, 822, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- HR_RECRUITER (2004) — 查看题库 + 提交与结果
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2004, 802, NOW()), (2004, 803, NOW()),
(2004, 821, NOW()), (2004, 822, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- INTERVIEWER (2005) — 查看题库 + 查询结果（不可提交）
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2005, 802, NOW()), (2005, 803, NOW()),
(2005, 822, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- CANDIDATE (3001) — 提交代码 + 查询结果
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(3001, 821, NOW()), (3001, 822, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

COMMIT;
