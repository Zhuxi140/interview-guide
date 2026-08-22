-- =============================================
-- Phase 9: sys_permissions + sys_role_permissions 初始化（增值与运营）
-- =============================================
-- 注意：sys_role_permissions 无 id 字段，主键为 (role_id, permission_id)

BEGIN;

-- ===== sys_permissions =====

INSERT INTO sys_permissions (id, perm_code, perm_type, api_path, status) VALUES
-- 9.1 日历空闲时段
(1101, 'enterprise:calendar-slots:create', 'API', '/api/v1/enterprises/*/calendar-slots', 1),
(1102, 'enterprise:calendar-slots:list',   'API', '/api/v1/enterprises/*/calendar-slots', 1),
(1103, 'enterprise:calendar-slots:delete', 'API', '/api/v1/enterprises/*/calendar-slots/*', 1),

-- 9.2 系统消息通知
(1104, 'notification:list',     'API', '/api/v1/notifications', 1),
(1105, 'notification:read',     'API', '/api/v1/notifications/*/read', 1),
(1106, 'notification:read-all', 'API', '/api/v1/notifications/read-all', 1),

-- 9.3 AI 答疑
(1107, 'candidate:tutor:create-session', 'API', '/api/v1/candidate/tutor/sessions', 1),
(1108, 'candidate:tutor:list-sessions',  'API', '/api/v1/candidate/tutor/sessions', 1),
(1109, 'candidate:tutor:send-message',   'API', '/api/v1/candidate/tutor/sessions/*/messages', 1),
(1110, 'candidate:tutor:list-messages',  'API', '/api/v1/candidate/tutor/sessions/*/messages', 1),

-- 9.4 Spark 离线语料任务
(1111, 'admin:spark-tasks:list',   'API', '/api/v1/admin/spark-tasks', 1),
(1112, 'admin:spark-tasks:detail', 'API', '/api/v1/admin/spark-tasks/*', 1),

-- 9.3 审计日志
(1113, 'admin:audit:api-logs:list',    'API', '/api/v1/admin/audit/api-logs', 1),
(1114, 'admin:audit:api-logs:detail',  'API', '/api/v1/admin/audit/api-logs/*', 1),
(1115, 'admin:audit:api-logs:archive', 'API', '/api/v1/admin/audit/api-logs/archive', 1),
(1116, 'admin:audit:operate-logs:list',    'API', '/api/v1/admin/audit/operate-logs', 1),
(1117, 'admin:audit:operate-logs:detail',  'API', '/api/v1/admin/audit/operate-logs/*', 1),
(1118, 'admin:audit:operate-logs:trace',   'API', '/api/v1/admin/audit/operate-logs/trace/*', 1),

-- 9.3 数据保留策略与归档任务
(1119, 'admin:data-retention:view',   'API', '/api/v1/admin/data-retention-policies', 1),
(1120, 'admin:data-retention:update', 'API', '/api/v1/admin/data-retention-policies', 1),
(1121, 'admin:archive-tasks:create',  'API', '/api/v1/admin/archive-tasks', 1),
(1122, 'admin:archive-tasks:list',    'API', '/api/v1/admin/archive-tasks', 1),
(1123, 'admin:archive-tasks:detail',  'API', '/api/v1/admin/archive-tasks/*', 1),

-- 9.3 企业操作审计日志
(1124, 'enterprise:audit:operate-logs:list',   'API', '/api/v1/enterprises/*/audit/operate-logs', 1),
(1125, 'enterprise:audit:operate-logs:detail', 'API', '/api/v1/enterprises/*/audit/operate-logs/*', 1),

-- 9.2 通知发送侧管理（Admin 渠道/模板/发送记录）
(1131, 'admin:notification-channels:view',    'API', '/api/v1/admin/notification-channels', 1),
(1132, 'admin:notification-channels:update',  'API', '/api/v1/admin/notification-channels', 1),
(1133, 'admin:notification-templates:list',   'API', '/api/v1/admin/notification-templates', 1),
(1134, 'admin:notification-templates:create', 'API', '/api/v1/admin/notification-templates', 1),
(1135, 'admin:notification-templates:update', 'API', '/api/v1/admin/notification-templates/*', 1),
(1136, 'admin:notifications:list',            'API', '/api/v1/admin/notifications', 1)
ON CONFLICT (id) DO UPDATE SET
    perm_code = EXCLUDED.perm_code,
    perm_type = EXCLUDED.perm_type,
    api_path = EXCLUDED.api_path,
    status = EXCLUDED.status;

-- ===== sys_role_permissions =====

-- SUPER_ADMIN (1001) — 全部 Phase 9 权限（含 1131-1136 通知发送侧管理）
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(1001, 1101, NOW()), (1001, 1102, NOW()), (1001, 1103, NOW()),
(1001, 1104, NOW()), (1001, 1105, NOW()), (1001, 1106, NOW()),
(1001, 1107, NOW()), (1001, 1108, NOW()), (1001, 1109, NOW()), (1001, 1110, NOW()),
(1001, 1111, NOW()), (1001, 1112, NOW()),
(1001, 1113, NOW()), (1001, 1114, NOW()), (1001, 1115, NOW()),
(1001, 1116, NOW()), (1001, 1117, NOW()), (1001, 1118, NOW()),
(1001, 1119, NOW()), (1001, 1120, NOW()), (1001, 1121, NOW()),
(1001, 1122, NOW()), (1001, 1123, NOW()),
(1001, 1124, NOW()), (1001, 1125, NOW()),
(1001, 1131, NOW()), (1001, 1132, NOW()), (1001, 1133, NOW()),
(1001, 1134, NOW()), (1001, 1135, NOW()), (1001, 1136, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ENTERPRISE_OWNER (2001) / ENTERPRISE_ADMIN (2002) — 企业操作审计日志
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2001, 1124, NOW()), (2001, 1125, NOW()),
(2002, 1124, NOW()), (2002, 1125, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ENTERPRISE_OWNER (2001) — 日历 + 通知
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2001, 1101, NOW()), (2001, 1102, NOW()), (2001, 1103, NOW()),
(2001, 1104, NOW()), (2001, 1105, NOW()), (2001, 1106, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ENTERPRISE_ADMIN (2002) — 日历 + 通知
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2002, 1101, NOW()), (2002, 1102, NOW()), (2002, 1103, NOW()),
(2002, 1104, NOW()), (2002, 1105, NOW()), (2002, 1106, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- HR_MANAGER (2003) — 日历 + 通知
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2003, 1101, NOW()), (2003, 1102, NOW()), (2003, 1103, NOW()),
(2003, 1104, NOW()), (2003, 1105, NOW()), (2003, 1106, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- HR_RECRUITER (2004) — 通知
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2004, 1104, NOW()), (2004, 1105, NOW()), (2004, 1106, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- INTERVIEWER (2005) — 日历 + 通知
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2005, 1101, NOW()), (2005, 1102, NOW()), (2005, 1103, NOW()),
(2005, 1104, NOW()), (2005, 1105, NOW()), (2005, 1106, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- CANDIDATE (3001) — 通知 + AI 答疑
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(3001, 1104, NOW()), (3001, 1105, NOW()), (3001, 1106, NOW()),
(3001, 1107, NOW()), (3001, 1108, NOW()), (3001, 1109, NOW()), (3001, 1110, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

COMMIT;
