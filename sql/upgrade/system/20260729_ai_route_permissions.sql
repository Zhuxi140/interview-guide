-- 将旧 LLM 单例设置权限切换为按模型类型管理的 AI 默认路由权限。

BEGIN;

UPDATE sys_permissions
SET perm_code = 'admin:ai:route:list',
    api_path = '/api/v1/admin/ai/routes'
WHERE perm_code = 'admin:llm:setting:detail';

UPDATE sys_permissions
SET perm_code = 'admin:ai:route:update',
    api_path = '/api/v1/admin/ai/routes/*'
WHERE perm_code = 'admin:llm:setting:update';

COMMIT;
