BEGIN;

INSERT INTO sys_permissions (id, perm_code, perm_type, api_path, status) VALUES
(417, 'application:screening-config:detail', 'API', '/api/v1/enterprises/*/jobs/*/screening-config', 1),
(418, 'application:screening-config:update', 'API', '/api/v1/enterprises/*/jobs/*/screening-config', 1),
(419, 'application:ai-screening:create', 'API', '/api/v1/enterprises/*/applications/*/ai-screenings', 1),
(420, 'application:ai-screening:detail', 'API', '/api/v1/enterprises/*/applications/*/ai-screenings/latest', 1),
(426, 'application:ai-screening:review', 'API', '/api/v1/enterprises/*/applications/*/ai-screenings/*/review', 1),
(427, 'application:candidate-profile:detail', 'API', '/api/v1/enterprises/*/applications/*/candidate-profile', 1),
(428, 'candidate:application:match-analysis:create', 'API', '/api/v1/candidate/applications/*/match-analyses', 1),
(429, 'candidate:application:match-analysis:detail', 'API', '/api/v1/candidate/applications/*/match-analyses/latest', 1)
ON CONFLICT (id) DO UPDATE SET
    perm_code = EXCLUDED.perm_code,
    perm_type = EXCLUDED.perm_type,
    api_path = EXCLUDED.api_path,
    status = EXCLUDED.status;

INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
-- 平台超级管理员
(1001, 417, NOW()), (1001, 418, NOW()), (1001, 419, NOW()), (1001, 420, NOW()),
(1001, 426, NOW()), (1001, 427, NOW()), (1001, 428, NOW()), (1001, 429, NOW()),
-- 企业所有者、管理员、HR 经理
(2001, 417, NOW()), (2001, 418, NOW()), (2001, 419, NOW()), (2001, 420, NOW()), (2001, 426, NOW()), (2001, 427, NOW()),
(2002, 417, NOW()), (2002, 418, NOW()), (2002, 419, NOW()), (2002, 420, NOW()), (2002, 426, NOW()), (2002, 427, NOW()),
(2003, 417, NOW()), (2003, 418, NOW()), (2003, 419, NOW()), (2003, 420, NOW()), (2003, 426, NOW()), (2003, 427, NOW()),
-- 招聘专员不允许修改阈值，但可执行、查看和审核初筛
(2004, 417, NOW()), (2004, 419, NOW()), (2004, 420, NOW()), (2004, 426, NOW()), (2004, 427, NOW()),
-- 候选人仅能访问本人岗位预测
(3001, 428, NOW()), (3001, 429, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

COMMIT;
