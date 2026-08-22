-- =============================================
-- Phase 7: sys_permissions + sys_role_permissions 初始化
-- =============================================
-- 注意：sys_role_permissions 无 id 字段，主键为 (role_id, permission_id)

-- ===== sys_permissions =====

INSERT INTO sys_permissions (id, perm_code, perm_type, api_path, status) VALUES
-- 7.1 知识库文档管理
(901, 'knowledge-base:create',    'API', '/api/v1/enterprises/*/knowledge-bases', 1),
(902, 'knowledge-base:list',      'API', '/api/v1/enterprises/*/knowledge-bases', 1),
(903, 'knowledge-base:detail',    'API', '/api/v1/enterprises/*/knowledge-bases/*', 1),
(904, 'knowledge-base:delete',    'API', '/api/v1/enterprises/*/knowledge-bases/*', 1),
(905, 'knowledge-base:vectorize', 'API', '/api/v1/enterprises/*/knowledge-bases/*/vectorize', 1),

-- 7.2 RAG 对话
(911, 'rag:session-create',       'API', '/api/v1/rag/sessions', 1),
(912, 'rag:session-list',         'API', '/api/v1/rag/sessions', 1),
(913, 'rag:message-send',         'API', '/api/v1/rag/sessions/*/messages', 1),
(914, 'rag:message-list',         'API', '/api/v1/rag/sessions/*/messages', 1),
(915, 'rag:session-knowledge-bind','API', '/api/v1/rag/sessions/*/knowledge-bind', 1)
ON CONFLICT (id) DO UPDATE SET
    perm_code = EXCLUDED.perm_code,
    perm_type = EXCLUDED.perm_type,
    api_path = EXCLUDED.api_path,
    status = EXCLUDED.status;

-- ===== sys_role_permissions =====

-- SUPER_ADMIN (1001) — 全部 10 个权限
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(1001, 901, NOW()), (1001, 902, NOW()), (1001, 903, NOW()), (1001, 904, NOW()), (1001, 905, NOW()),
(1001, 911, NOW()), (1001, 912, NOW()), (1001, 913, NOW()), (1001, 914, NOW()), (1001, 915, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- FINANCE_ADMIN (1002) — 无知识库权限

-- ENTERPRISE_OWNER (2001) — 知识库全部 + RAG 全部
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2001, 901, NOW()), (2001, 902, NOW()), (2001, 903, NOW()), (2001, 904, NOW()), (2001, 905, NOW()),
(2001, 911, NOW()), (2001, 912, NOW()), (2001, 913, NOW()), (2001, 914, NOW()), (2001, 915, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ENTERPRISE_ADMIN (2002) — 与 OWNER 一致
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2002, 901, NOW()), (2002, 902, NOW()), (2002, 903, NOW()), (2002, 904, NOW()), (2002, 905, NOW()),
(2002, 911, NOW()), (2002, 912, NOW()), (2002, 913, NOW()), (2002, 914, NOW()), (2002, 915, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- HR_MANAGER (2003) — 知识库全部 + RAG 全部
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2003, 901, NOW()), (2003, 902, NOW()), (2003, 903, NOW()), (2003, 904, NOW()), (2003, 905, NOW()),
(2003, 911, NOW()), (2003, 912, NOW()), (2003, 913, NOW()), (2003, 914, NOW()), (2003, 915, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- HR_RECRUITER (2004) — 知识库查看/上传 + RAG 使用
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2004, 901, NOW()), (2004, 902, NOW()), (2004, 903, NOW()),
(2004, 911, NOW()), (2004, 912, NOW()), (2004, 913, NOW()), (2004, 914, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- INTERVIEWER (2005) — 知识库查看 + RAG 使用
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2005, 902, NOW()), (2005, 903, NOW()),
(2005, 911, NOW()), (2005, 912, NOW()), (2005, 913, NOW()), (2005, 914, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- CANDIDATE (3001) — RAG 使用（面试中检索知识库）
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(3001, 911, NOW()), (3001, 912, NOW()), (3001, 913, NOW()), (3001, 914, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;
