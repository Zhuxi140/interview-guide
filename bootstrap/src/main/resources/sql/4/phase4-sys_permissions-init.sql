-- =============================================
-- Phase 4：计费权限与角色授权初始化
-- =============================================
-- 说明：
-- 1. 普通 SKU 查询只要求登录，不配置业务权限。
-- 2. 支付回调通过渠道签名鉴权，不进入 JWT/RBAC 权限链路。
-- 3. sys_role_permissions 的主键为 (role_id, permission_id)，没有 id 字段。

BEGIN;

-- ===== sys_permissions =====

INSERT INTO sys_permissions (id, perm_code, perm_type, api_path, status)
VALUES
    -- Admin：SKU 管理
    (601, 'admin:billing:sku:list',   'API', '/api/v1/admin/billing/skus',          1),
    (602, 'admin:billing:sku:detail', 'API', '/api/v1/admin/billing/skus/*',        1),
    (603, 'admin:billing:sku:create', 'API', '/api/v1/admin/billing/skus',          1),
    (604, 'admin:billing:sku:update', 'API', '/api/v1/admin/billing/skus/*',        1),
    (605, 'admin:billing:sku:status', 'API', '/api/v1/admin/billing/skus/*/status', 1),

    -- Enterprise：充值订单
    (611, 'billing:order:create', 'API', '/api/v1/enterprises/*/billing/orders',                   1),
    (612, 'billing:order:pay',    'API', '/api/v1/enterprises/*/billing/orders/*/payment-intents', 1),
    (613, 'billing:order:list',   'API', '/api/v1/enterprises/*/billing/orders',                   1),
    (614, 'billing:order:detail', 'API', '/api/v1/enterprises/*/billing/orders/*',                 1),
    (615, 'billing:order:cancel', 'API', '/api/v1/enterprises/*/billing/orders/*/cancel',          1),

    -- Admin：充值订单管理
    (616, 'admin:billing:order:list',             'API', '/api/v1/admin/billing/orders',                    1),
    (617, 'admin:billing:order:detail',           'API', '/api/v1/admin/billing/orders/*',                  1),
    (618, 'admin:billing:order:simulate-payment', 'API', '/api/v1/admin/billing/orders/*/simulate-payment', 1),

    -- Enterprise：钱包与算力消耗明细
    (621, 'billing:wallet:detail',       'API', '/api/v1/enterprises/*/billing/wallet',             1),
    (622, 'billing:wallet:transactions', 'API', '/api/v1/enterprises/*/billing/wallet/transactions', 1),
    (623, 'billing:consume-log:list',    'API', '/api/v1/enterprises/*/billing/token-consume-logs',  1)
ON CONFLICT (perm_code) DO UPDATE
SET perm_type = EXCLUDED.perm_type,
    api_path  = EXCLUDED.api_path,
    status    = EXCLUDED.status;

-- ===== sys_role_permissions =====

-- 平台角色只拥有平台计费权限，不混入企业租户权限。
-- SUPER_ADMIN (1001)：全部平台计费权限
INSERT INTO sys_role_permissions (role_id, permission_id, created_at)
SELECT 1001, id, CURRENT_TIMESTAMP
FROM sys_permissions
WHERE perm_code IN (
    'admin:billing:sku:list',
    'admin:billing:sku:detail',
    'admin:billing:sku:create',
    'admin:billing:sku:update',
    'admin:billing:sku:status',
    'admin:billing:order:list',
    'admin:billing:order:detail',
    'admin:billing:order:simulate-payment'
)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- FINANCE_ADMIN (1002)：全部平台计费权限
INSERT INTO sys_role_permissions (role_id, permission_id, created_at)
SELECT 1002, id, CURRENT_TIMESTAMP
FROM sys_permissions
WHERE perm_code IN (
    'admin:billing:sku:list',
    'admin:billing:sku:detail',
    'admin:billing:sku:create',
    'admin:billing:sku:update',
    'admin:billing:sku:status',
    'admin:billing:order:list',
    'admin:billing:order:detail',
    'admin:billing:order:simulate-payment'
)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ENTERPRISE_OWNER (2001)：下单、支付、取消及全部企业计费查询权限
INSERT INTO sys_role_permissions (role_id, permission_id, created_at)
SELECT 2001, id, CURRENT_TIMESTAMP
FROM sys_permissions
WHERE perm_code IN (
    'billing:order:create',
    'billing:order:pay',
    'billing:order:list',
    'billing:order:detail',
    'billing:order:cancel',
    'billing:wallet:detail',
    'billing:wallet:transactions',
    'billing:consume-log:list'
)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ENTERPRISE_ADMIN (2002)：只读查看订单、钱包及算力消耗明细
INSERT INTO sys_role_permissions (role_id, permission_id, created_at)
SELECT 2002, id, CURRENT_TIMESTAMP
FROM sys_permissions
WHERE perm_code IN (
    'billing:order:list',
    'billing:order:detail',
    'billing:wallet:detail',
    'billing:wallet:transactions',
    'billing:consume-log:list'
)
ON CONFLICT (role_id, permission_id) DO NOTHING;

COMMIT;
