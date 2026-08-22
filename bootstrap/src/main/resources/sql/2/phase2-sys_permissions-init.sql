-- 第二阶段：sys_permissions + sys_role_permissions 初始化
-- sys_role_permissions 无 id 字段，主键为 (role_id, permission_id)。
-- 本阶段占用 401-443；425 为已下线的任意消息状态修改权限。

BEGIN;

-- 删除已经下线的本地消息任意状态修改权限。
DELETE FROM sys_role_permissions
WHERE permission_id IN (
    SELECT id
    FROM sys_permissions
    WHERE id = 425 OR perm_code = 'ops:local-message:status'
);

DELETE FROM sys_permissions
WHERE id = 425 OR perm_code = 'ops:local-message:status';

-- 第二阶段接口权限。
INSERT INTO sys_permissions (id, perm_code, perm_type, api_path, status) VALUES
-- 2.1 候选人本人简历
(401, 'resume:upload',
 'API', '/api/v1/resumes', 1),
(402, 'resume:list',
 'API', '/api/v1/resumes', 1),
(403, 'resume:detail',
 'API', '/api/v1/resumes/*', 1),
(404, 'resume:delete',
 'API', '/api/v1/resumes/*', 1),
(405, 'resume:analyze',
 'API', '/api/v1/resumes/*/analyze', 1),
(406, 'resume:analysis-result',
 'API', '/api/v1/resumes/*/analysis', 1),
(407, 'resume:download',
 'API', '/api/v1/resumes/*/download', 1),

-- 2.2 投递与初筛
(411, 'application:apply',
 'API', '/api/v1/jobs/*/applications', 1),
(412, 'application:list',
 'API', '/api/v1/enterprises/*/jobs/*/applications', 1),
(413, 'application:detail',
 'API', '/api/v1/enterprises/*/applications/*', 1),
(414, 'application:update-status',
 'API', '/api/v1/enterprises/*/applications/*/status', 1),
(415, 'candidate:applications',
 'API', '/api/v1/candidate/applications', 1),
(416, 'candidate:application:withdraw',
 'API', '/api/v1/candidate/applications/*/withdraw', 1),
(417, 'application:screening-config:detail',
 'API', '/api/v1/enterprises/*/jobs/*/screening-config', 1),
(418, 'application:screening-config:update',
 'API', '/api/v1/enterprises/*/jobs/*/screening-config', 1),
(419, 'application:ai-screening:create',
 'API', '/api/v1/enterprises/*/applications/*/ai-screenings', 1),
(420, 'application:ai-screening:detail',
 'API', '/api/v1/enterprises/*/applications/*/ai-screenings/latest', 1),
(426, 'application:ai-screening:review',
 'API', '/api/v1/enterprises/*/applications/*/ai-screenings/*/review', 1),
(427, 'application:candidate-profile:detail',
 'API', '/api/v1/enterprises/*/applications/*/candidate-profile', 1),
(428, 'candidate:application:match-analysis:create',
 'API', '/api/v1/candidate/applications/*/match-analyses', 1),
(429, 'candidate:application:match-analysis:detail',
 'API', '/api/v1/candidate/applications/*/match-analyses/latest', 1),

-- 2.5 本地消息运维
(421, 'ops:local-message:page',
 'API', '/api/v1/admin/local-messages', 1),
(422, 'ops:local-message:detail',
 'API', '/api/v1/admin/local-messages/*', 1),
(423, 'ops:local-message:retry',
 'API', '/api/v1/admin/local-messages/*/retry', 1),
(424, 'ops:local-message:batch-retry',
 'API', '/api/v1/admin/local-messages/batch-retry', 1),

-- 2.4 大模型 Provider 与全局路由配置
(431, 'admin:llm:provider:create',
 'API', '/api/v1/admin/llm/providers', 1),
(432, 'admin:llm:provider:list',
 'API', '/api/v1/admin/llm/providers', 1),
(433, 'admin:llm:provider:detail',
 'API', '/api/v1/admin/llm/providers/*', 1),
(434, 'admin:llm:provider:update',
 'API', '/api/v1/admin/llm/providers/*', 1),
(435, 'admin:llm:provider:status',
 'API', '/api/v1/admin/llm/providers/*/status', 1),
(436, 'admin:llm:provider:delete',
 'API', '/api/v1/admin/llm/providers/*', 1),
(437, 'admin:llm:provider:test',
 'API', '/api/v1/admin/llm/providers/*/test-connection', 1),
(438, 'admin:ai:route:list',
 'API', '/api/v1/admin/ai/routes', 1),
(439, 'admin:ai:route:update',
 'API', '/api/v1/admin/ai/routes/*', 1),
(440, 'admin:llm:scene:list',
 'API', '/api/v1/admin/llm/scenes', 1),
(441, 'admin:llm:scene:detail',
 'API', '/api/v1/admin/llm/scenes/*', 1),
(442, 'admin:llm:scene:update',
 'API', '/api/v1/admin/llm/scenes/*', 1),
(443, 'admin:llm:scene:status',
 'API', '/api/v1/admin/llm/scenes/*/status', 1),

-- 2.6 批量简历导入
(444, 'resume:import-batch:create',
 'API', '/api/v1/enterprises/*/resume-import-batches', 1),
(445, 'resume:import-batch:detail',
 'API', '/api/v1/enterprises/*/resume-import-batches/*', 1),
(446, 'resume:import-batch:items',
 'API', '/api/v1/enterprises/*/resume-import-batches/*/items', 1),
(447, 'resume:import-batch:retry',
 'API', '/api/v1/enterprises/*/resume-import-batches/*/retry-failed', 1),

-- 2.7 企业人才池
(448, 'enterprise:candidates:list',
 'API', '/api/v1/enterprises/*/candidates', 1),
(449, 'enterprise:candidates:detail',
 'API', '/api/v1/enterprises/*/candidates/*', 1),
(450, 'enterprise:candidates:overview',
 'API', '/api/v1/enterprises/*/candidates/*/overview', 1)
ON CONFLICT (id) DO UPDATE SET
    perm_code = EXCLUDED.perm_code,
    perm_type = EXCLUDED.perm_type,
    api_path = EXCLUDED.api_path,
    status = EXCLUDED.status;

-- 重建本阶段角色授权，清除旧脚本遗留的越权关系。
DELETE FROM sys_role_permissions
WHERE permission_id BETWEEN 401 AND 443;

-- SUPER_ADMIN：拥有全部第二阶段权限。
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(1001, 401, NOW()), (1001, 402, NOW()), (1001, 403, NOW()),
(1001, 404, NOW()), (1001, 405, NOW()), (1001, 406, NOW()),
(1001, 407, NOW()),
(1001, 411, NOW()), (1001, 412, NOW()), (1001, 413, NOW()),
(1001, 414, NOW()), (1001, 415, NOW()), (1001, 416, NOW()),
(1001, 417, NOW()), (1001, 418, NOW()), (1001, 419, NOW()),
(1001, 420, NOW()), (1001, 426, NOW()), (1001, 427, NOW()),
(1001, 428, NOW()), (1001, 429, NOW()),
(1001, 421, NOW()), (1001, 422, NOW()), (1001, 423, NOW()),
(1001, 424, NOW()),
(1001, 431, NOW()), (1001, 432, NOW()), (1001, 433, NOW()),
(1001, 434, NOW()), (1001, 435, NOW()), (1001, 436, NOW()),
(1001, 437, NOW()), (1001, 438, NOW()), (1001, 439, NOW()),
(1001, 440, NOW()), (1001, 441, NOW()), (1001, 442, NOW()),
(1001, 443, NOW()),
(1001, 444, NOW()), (1001, 445, NOW()), (1001, 446, NOW()),
(1001, 447, NOW()), (1001, 448, NOW()), (1001, 449, NOW()),
(1001, 450, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- PLATFORM_OPS：本地消息运维、Provider 脱敏查询和连接测试。
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(1003, 421, NOW()), (1003, 422, NOW()), (1003, 423, NOW()),
(1003, 424, NOW()),
(1003, 432, NOW()), (1003, 433, NOW()), (1003, 437, NOW()),
(1003, 438, NOW()), (1003, 440, NOW()), (1003, 441, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 企业招聘角色：只访问本企业的投递资源，仍需接口层校验 enterpriseId。
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2001, 412, NOW()), (2001, 413, NOW()), (2001, 414, NOW()),
(2001, 417, NOW()), (2001, 418, NOW()), (2001, 419, NOW()),
(2001, 420, NOW()), (2001, 426, NOW()), (2001, 427, NOW()),
(2002, 412, NOW()), (2002, 413, NOW()), (2002, 414, NOW()),
(2002, 417, NOW()), (2002, 418, NOW()), (2002, 419, NOW()),
(2002, 420, NOW()), (2002, 426, NOW()), (2002, 427, NOW()),
(2003, 412, NOW()), (2003, 413, NOW()), (2003, 414, NOW()),
(2003, 417, NOW()), (2003, 418, NOW()), (2003, 419, NOW()),
(2003, 420, NOW()), (2003, 426, NOW()), (2003, 427, NOW()),
(2004, 412, NOW()), (2004, 413, NOW()), (2004, 414, NOW()),
(2004, 417, NOW()), (2004, 419, NOW()), (2004, 420, NOW()),
(2004, 426, NOW()), (2004, 427, NOW()),
(2005, 412, NOW()), (2005, 413, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 企业 HR 角色：批量简历导入与企业人才池
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2001, 444, NOW()), (2001, 445, NOW()), (2001, 446, NOW()),
(2001, 447, NOW()), (2001, 448, NOW()), (2001, 449, NOW()),
(2001, 450, NOW()),
(2002, 444, NOW()), (2002, 445, NOW()), (2002, 446, NOW()),
(2002, 447, NOW()), (2002, 448, NOW()), (2002, 449, NOW()),
(2002, 450, NOW()),
(2003, 444, NOW()), (2003, 445, NOW()), (2003, 446, NOW()),
(2003, 447, NOW()), (2003, 448, NOW()), (2003, 449, NOW()),
(2003, 450, NOW()),
(2004, 444, NOW()), (2004, 445, NOW()), (2004, 446, NOW()),
(2004, 447, NOW()), (2004, 448, NOW()), (2004, 449, NOW()),
(2004, 450, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 候选人：管理本人简历、投递岗位、查看并撤回本人投递。
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(3001, 401, NOW()), (3001, 402, NOW()), (3001, 403, NOW()),
(3001, 404, NOW()), (3001, 405, NOW()), (3001, 406, NOW()),
(3001, 407, NOW()),
(3001, 411, NOW()), (3001, 415, NOW()), (3001, 416, NOW()),
(3001, 428, NOW()), (3001, 429, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

COMMIT;
