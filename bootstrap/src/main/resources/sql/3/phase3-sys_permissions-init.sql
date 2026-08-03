-- ============================================================
-- Phase 3: sys_permissions + sys_role_permissions 初始化
-- ============================================================
-- 注意：
-- 1. sys_role_permissions 主键为 (role_id, permission_id)，没有独立 id。
-- 2. API 路径与阶段三当前 REST 契约保持一致。
-- 3. OFFERED / HIRED / REJECTED 属于投递聚合，复用 Phase 2 的
--    application:update-status，不再定义面试排期招聘终态权限。

BEGIN;

-- 重跑脚本时先清理 Phase 3 角色授权，确保角色权限集合可以收敛。
DELETE FROM sys_role_permissions
WHERE permission_id IN (
    501, 502, 503, 504, 505, 506, 507,
    511, 512, 513, 514, 515, 516, 517, 518, 519,
    521, 522, 523, 524, 525, 526,
    531, 532, 533, 534, 535, 536
);

-- 删除已取消的“候选人主动触发追问”权限。
DELETE FROM sys_permissions
WHERE id = 524 OR perm_code = 'interview-session:follow-up';

-- ==================== sys_permissions ====================
INSERT INTO sys_permissions (id, perm_code, perm_type, api_path, status) VALUES
-- 3.1 面试模板与组卷
(501, 'interview-template:create',
 'API', '/api/v1/enterprises/*/interview-templates', 1),
(502, 'interview-template:list',
 'API', '/api/v1/enterprises/*/interview-templates', 1),
(503, 'interview-template:detail',
 'API', '/api/v1/enterprises/*/interview-templates/*', 1),
(504, 'interview-template:update',
 'API', '/api/v1/enterprises/*/interview-templates/*', 1),
(505, 'interview-template:delete',
 'API', '/api/v1/enterprises/*/interview-templates/*', 1),
(506, 'interview-template:phase-config:update',
 'API', '/api/v1/enterprises/*/interview-templates/*/phase-configs/*', 1),
(507, 'interview-template:phase-configs:list',
 'API', '/api/v1/enterprises/*/interview-templates/*/phase-configs', 1),

-- 3.2 企业端面试排期
(511, 'interview-schedule:create',
 'API', '/api/v1/enterprises/*/applications/*/interview-schedules', 1),
(512, 'interview-schedule:ai-suggest',
 'API', '/api/v1/enterprises/*/applications/*/interview-plan-drafts', 1),
(513, 'interview-schedule:list',
 'API', '/api/v1/enterprises/*/interview-schedules', 1),
(514, 'interview-schedule:detail',
 'API', '/api/v1/enterprises/*/interview-schedules/*', 1),
(515, 'interview-schedule:reschedule',
 'API', '/api/v1/enterprises/*/interview-schedules/*', 1),
(519, 'interview-schedule:cancel',
 'API', '/api/v1/enterprises/*/interview-schedules/*/cancel', 1),

-- 3.2 候选人端面试排期
(516, 'candidate:interview-schedules',
 'API', '/api/v1/candidate/interview-schedules', 1),
(517, 'candidate:interview-schedule:decision',
 'API', '/api/v1/candidate/interview-schedules/*/decision', 1),
(518, 'candidate:interview-schedule:cancel',
 'API', '/api/v1/candidate/interview-schedules/*/cancel', 1),

-- 3.3 面试会话
(521, 'interview-session:start',
 'API', '/api/v1/interview-sessions', 1),
(522, 'interview-session:detail',
 'API', '/api/v1/interview-sessions/*', 1),
(523, 'interview-session:answer',
 'API', '/api/v1/interview-sessions/*/answers', 1),
(525, 'interview-session:history',
 'API', '/api/v1/interview-sessions/*/history', 1),
(526, 'interview-session:end',
 'API', '/api/v1/interview-sessions/*/end', 1),

-- 3.4 面试报告与投递状态日志
(531, 'interview-report:detail',
 'API', '/api/v1/enterprises/*/interview-schedules/*/report', 1),
(532, 'enterprise:interview-reports',
 'API', '/api/v1/enterprises/*/interview-reports', 1),
(533, 'enterprise:application-transition-logs',
 'API', '/api/v1/enterprises/*/workflow-logs', 1),
(534, 'candidate:interview-reports',
 'API', '/api/v1/candidate/interview-reports', 1),
(535, 'candidate:interview-report:download',
 'API', '/api/v1/candidate/interview-schedules/*/report/download', 1),
(536, 'candidate:interview-report:detail',
 'API', '/api/v1/candidate/interview-schedules/*/report', 1)
ON CONFLICT (id) DO UPDATE SET
    perm_code = EXCLUDED.perm_code,
    perm_type = EXCLUDED.perm_type,
    api_path = EXCLUDED.api_path,
    status = EXCLUDED.status;

-- ==================== sys_role_permissions ====================

-- SUPER_ADMIN (1001)：全部 27 个 Phase 3 权限
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(1001, 501, NOW()), (1001, 502, NOW()), (1001, 503, NOW()),
(1001, 504, NOW()), (1001, 505, NOW()), (1001, 506, NOW()),
(1001, 507, NOW()),
(1001, 511, NOW()), (1001, 512, NOW()), (1001, 513, NOW()),
(1001, 514, NOW()), (1001, 515, NOW()), (1001, 516, NOW()),
(1001, 517, NOW()), (1001, 518, NOW()), (1001, 519, NOW()),
(1001, 521, NOW()), (1001, 522, NOW()), (1001, 523, NOW()),
(1001, 525, NOW()), (1001, 526, NOW()),
(1001, 531, NOW()), (1001, 532, NOW()), (1001, 533, NOW()),
(1001, 534, NOW()), (1001, 535, NOW()), (1001, 536, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- FINANCE_ADMIN (1002)：仅查看企业报告列表
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(1002, 532, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ENTERPRISE_OWNER (2001)：企业模板、排期、报告与流转日志
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2001, 501, NOW()), (2001, 502, NOW()), (2001, 503, NOW()),
(2001, 504, NOW()), (2001, 505, NOW()), (2001, 506, NOW()),
(2001, 507, NOW()),
(2001, 511, NOW()), (2001, 512, NOW()), (2001, 513, NOW()),
(2001, 514, NOW()), (2001, 515, NOW()), (2001, 519, NOW()),
(2001, 531, NOW()), (2001, 532, NOW()), (2001, 533, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ENTERPRISE_ADMIN (2002)：与企业所有者一致
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2002, 501, NOW()), (2002, 502, NOW()), (2002, 503, NOW()),
(2002, 504, NOW()), (2002, 505, NOW()), (2002, 506, NOW()),
(2002, 507, NOW()),
(2002, 511, NOW()), (2002, 512, NOW()), (2002, 513, NOW()),
(2002, 514, NOW()), (2002, 515, NOW()), (2002, 519, NOW()),
(2002, 531, NOW()), (2002, 532, NOW()), (2002, 533, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- HR_MANAGER (2003)：模板、排期、报告与流转日志
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2003, 501, NOW()), (2003, 502, NOW()), (2003, 503, NOW()),
(2003, 504, NOW()), (2003, 505, NOW()), (2003, 506, NOW()),
(2003, 507, NOW()),
(2003, 511, NOW()), (2003, 512, NOW()), (2003, 513, NOW()),
(2003, 514, NOW()), (2003, 515, NOW()), (2003, 519, NOW()),
(2003, 531, NOW()), (2003, 532, NOW()), (2003, 533, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- HR_RECRUITER (2004)：模板、排期、报告与流转日志
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2004, 501, NOW()), (2004, 502, NOW()), (2004, 503, NOW()),
(2004, 504, NOW()), (2004, 505, NOW()), (2004, 506, NOW()),
(2004, 507, NOW()),
(2004, 511, NOW()), (2004, 512, NOW()), (2004, 513, NOW()),
(2004, 514, NOW()), (2004, 515, NOW()), (2004, 519, NOW()),
(2004, 531, NOW()), (2004, 532, NOW()), (2004, 533, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- INTERVIEWER (2005)：查看模板、排期、会话、报告与流转日志
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2005, 502, NOW()), (2005, 503, NOW()), (2005, 507, NOW()),
(2005, 513, NOW()), (2005, 514, NOW()),
(2005, 522, NOW()), (2005, 525, NOW()),
(2005, 531, NOW()), (2005, 532, NOW()), (2005, 533, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- CANDIDATE (3001)：本人排期、会话和报告
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(3001, 516, NOW()), (3001, 517, NOW()), (3001, 518, NOW()),
(3001, 521, NOW()), (3001, 522, NOW()), (3001, 523, NOW()),
(3001, 525, NOW()), (3001, 526, NOW()),
(3001, 534, NOW()), (3001, 535, NOW()), (3001, 536, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

COMMIT;
