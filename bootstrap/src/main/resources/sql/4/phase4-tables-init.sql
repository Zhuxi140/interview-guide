-- 第四阶段：商业化闭环表结构初始化
-- PostgreSQL；跨模块关联仅使用逻辑外键，不创建物理 FOREIGN KEY。

BEGIN;

-- 算力套餐 SKU 定义表
CREATE TABLE IF NOT EXISTS billing_sku_catalog
(
    id              BIGINT         PRIMARY KEY,
    package_name    VARCHAR(64)    NOT NULL,
    description     TEXT,
    price           DECIMAL(10, 2) NOT NULL,
    currency        CHAR(3)        NOT NULL DEFAULT 'CNY',
    tokens_included BIGINT         NOT NULL,
    is_active       BOOLEAN        NOT NULL DEFAULT TRUE,
    version         INT            NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted      BOOLEAN        NOT NULL DEFAULT FALSE,
    updated_by      BIGINT,
    trace_id        VARCHAR(128),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_billing_sku_price
        CHECK (price > 0),
    CONSTRAINT chk_billing_sku_tokens
        CHECK (tokens_included > 0),
    CONSTRAINT chk_billing_sku_version
        CHECK (version >= 0),
    CONSTRAINT chk_billing_sku_currency
        CHECK (currency = 'CNY')
);

COMMENT ON TABLE billing_sku_catalog IS '算力套餐 SKU 定义表';
COMMENT ON COLUMN billing_sku_catalog.id IS '雪花主键';
COMMENT ON COLUMN billing_sku_catalog.updated_by IS '逻辑关联 sys_users.id';

CREATE UNIQUE INDEX IF NOT EXISTS uq_billing_sku_package_currency
    ON billing_sku_catalog (package_name, currency)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_billing_sku_active_created
    ON billing_sku_catalog (is_active, created_at DESC, id DESC)
    WHERE is_deleted = FALSE;

-- 企业算力钱包表
CREATE TABLE IF NOT EXISTS user_wallets
(
    id               BIGINT      PRIMARY KEY,
    enterprise_id    BIGINT      NOT NULL,
    balance          BIGINT      NOT NULL DEFAULT 0,
    frozen_balance   BIGINT      NOT NULL DEFAULT 0,
    total_recharged  BIGINT      NOT NULL DEFAULT 0,
    version          INT         NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_wallets_enterprise
        UNIQUE (enterprise_id),
    CONSTRAINT chk_user_wallets_balance
        CHECK (balance >= 0),
    CONSTRAINT chk_user_wallets_frozen_balance
        CHECK (frozen_balance >= 0),
    CONSTRAINT chk_user_wallets_total_recharged
        CHECK (total_recharged >= 0),
    CONSTRAINT chk_user_wallets_version
        CHECK (version >= 0)
);

COMMENT ON TABLE user_wallets IS '企业算力钱包表；钱包归属企业，不绑定某个管理员';
COMMENT ON COLUMN user_wallets.id IS '雪花主键';
COMMENT ON COLUMN user_wallets.enterprise_id IS '逻辑关联 enterprises.id';

-- 算力充值订单表
CREATE TABLE IF NOT EXISTS payment_orders
(
    id                             BIGINT         PRIMARY KEY,
    order_no                       VARCHAR(64)    NOT NULL,
    enterprise_id                  BIGINT         NOT NULL,
    user_id                        BIGINT         NOT NULL,
    sku_id                         BIGINT         NOT NULL,
    idempotency_key                VARCHAR(128)   NOT NULL,
    sku_snapshot                   JSONB          NOT NULL,
    amount                         DECIMAL(10, 2) NOT NULL,
    currency                       CHAR(3)        NOT NULL,
    tokens_granted                 BIGINT         NOT NULL,
    payment_provider               VARCHAR(32),
    provider_payment_id            VARCHAR(128),
    payment_intent_idempotency_key VARCHAR(128),
    payment_intent_snapshot        JSONB,
    external_transaction_id        VARCHAR(128),
    status                         VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    expire_time                    TIMESTAMPTZ    NOT NULL,
    paid_at                        TIMESTAMPTZ,
    cancelled_at                   TIMESTAMPTZ,
    expired_at                     TIMESTAMPTZ,
    status_reason                  VARCHAR(256),
    created_at                     TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                     TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trace_id                       VARCHAR(128),
    CONSTRAINT uq_payment_orders_order_no
        UNIQUE (order_no),
    CONSTRAINT chk_payment_orders_amount
        CHECK (amount > 0),
    CONSTRAINT chk_payment_orders_tokens
        CHECK (tokens_granted > 0),
    CONSTRAINT chk_payment_orders_expire_time
        CHECK (expire_time > created_at),
    CONSTRAINT chk_payment_orders_status
        CHECK (status IN ('PENDING', 'PAID', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT chk_payment_orders_paid_fields
        CHECK (
            status <> 'PAID'
            OR (external_transaction_id IS NOT NULL AND paid_at IS NOT NULL)
        ),
    CONSTRAINT chk_payment_orders_provider_fields
        CHECK (provider_payment_id IS NULL OR payment_provider IS NOT NULL)
);

COMMENT ON TABLE payment_orders IS '企业算力充值订单表';
COMMENT ON COLUMN payment_orders.id IS '雪花主键';
COMMENT ON COLUMN payment_orders.enterprise_id IS '逻辑关联 enterprises.id';
COMMENT ON COLUMN payment_orders.user_id IS '逻辑关联 sys_users.id，下单操作人';
COMMENT ON COLUMN payment_orders.sku_id IS '逻辑关联 billing_sku_catalog.id';
COMMENT ON COLUMN payment_orders.sku_snapshot IS '下单时的套餐不可变快照';

CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_orders_enterprise_idempotency
    ON payment_orders (enterprise_id, idempotency_key);

CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_orders_enterprise_intent_key
    ON payment_orders (enterprise_id, payment_intent_idempotency_key)
    WHERE payment_intent_idempotency_key IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_orders_external_transaction
    ON payment_orders (external_transaction_id)
    WHERE external_transaction_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_orders_provider_payment
    ON payment_orders (payment_provider, provider_payment_id)
    WHERE provider_payment_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_payment_orders_enterprise_created
    ON payment_orders (enterprise_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_payment_orders_pending_expire
    ON payment_orders (expire_time, id)
    WHERE status = 'PENDING';

-- 钱包流水明细表：唯一余额事实来源，只追加、不修改、不逻辑删除。
CREATE TABLE IF NOT EXISTS wallet_transactions
(
    id                    BIGINT       PRIMARY KEY,
    enterprise_id         BIGINT       NOT NULL,
    wallet_id             BIGINT       NOT NULL,
    command_id            VARCHAR(160) NOT NULL,
    parent_transaction_id BIGINT,
    balance_change        BIGINT       NOT NULL,
    frozen_change         BIGINT       NOT NULL DEFAULT 0,
    type                  VARCHAR(20)  NOT NULL,
    reference_type        VARCHAR(32),
    reference_id          BIGINT,
    balance_after         BIGINT       NOT NULL,
    frozen_after          BIGINT       NOT NULL,
    operator_user_id      BIGINT,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trace_id              VARCHAR(128),
    CONSTRAINT uq_wallet_transactions_command
        UNIQUE (command_id),
    CONSTRAINT chk_wallet_transactions_type
        CHECK (type IN ('FREEZE', 'UNFREEZE', 'CONSUME', 'RECHARGE')),
    CONSTRAINT chk_wallet_transactions_balance_after
        CHECK (balance_after >= 0),
    CONSTRAINT chk_wallet_transactions_frozen_after
        CHECK (frozen_after >= 0),
    CONSTRAINT chk_wallet_transactions_reference
        CHECK (
            (reference_type IS NULL AND reference_id IS NULL)
            OR (reference_type IS NOT NULL AND reference_id IS NOT NULL)
        ),
    CONSTRAINT chk_wallet_transactions_shape
        CHECK (
            (type = 'RECHARGE'
                AND balance_change > 0
                AND frozen_change = 0
                AND parent_transaction_id IS NULL)
            OR
            (type = 'FREEZE'
                AND balance_change < 0
                AND frozen_change = -balance_change
                AND parent_transaction_id IS NULL)
            OR
            (type = 'CONSUME'
                AND frozen_change < 0
                AND parent_transaction_id IS NOT NULL)
            OR
            (type = 'UNFREEZE'
                AND balance_change > 0
                AND frozen_change = -balance_change
                AND parent_transaction_id IS NOT NULL)
        )
);

COMMENT ON TABLE wallet_transactions IS '钱包权威账本；每行代表已提交的钱包变化';
COMMENT ON COLUMN wallet_transactions.id IS '雪花主键';
COMMENT ON COLUMN wallet_transactions.enterprise_id IS '逻辑关联 enterprises.id';
COMMENT ON COLUMN wallet_transactions.wallet_id IS '逻辑关联 user_wallets.id';
COMMENT ON COLUMN wallet_transactions.parent_transaction_id IS '逻辑关联冻结流水 wallet_transactions.id';
COMMENT ON COLUMN wallet_transactions.operator_user_id IS '逻辑关联 sys_users.id；系统任务可为 0';

CREATE UNIQUE INDEX IF NOT EXISTS uq_wallet_transactions_terminal_parent
    ON wallet_transactions (parent_transaction_id)
    WHERE type IN ('CONSUME', 'UNFREEZE');

CREATE INDEX IF NOT EXISTS idx_wallet_transactions_wallet_created
    ON wallet_transactions (wallet_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_wallet_transactions_enterprise_created
    ON wallet_transactions (enterprise_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_wallet_transactions_reference
    ON wallet_transactions (reference_type, reference_id)
    WHERE reference_id IS NOT NULL;

-- AI 算力消耗明细表，不作为余额事实来源。
CREATE TABLE IF NOT EXISTS token_consume_logs
(
    id                    BIGINT       PRIMARY KEY,
    enterprise_id         BIGINT       NOT NULL,
    user_id               BIGINT       NOT NULL,
    wallet_transaction_id BIGINT       NOT NULL,
    command_id            VARCHAR(160) NOT NULL,
    biz_type              VARCHAR(32)  NOT NULL,
    biz_id                BIGINT       NOT NULL,
    attempt_no            INT          NOT NULL DEFAULT 1,
    tokens_consumed       BIGINT       NOT NULL,
    balance_after         BIGINT       NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trace_id              VARCHAR(128),
    CONSTRAINT uq_token_consume_wallet_transaction
        UNIQUE (wallet_transaction_id),
    CONSTRAINT uq_token_consume_command
        UNIQUE (command_id),
    CONSTRAINT chk_token_consume_attempt
        CHECK (attempt_no > 0),
    CONSTRAINT chk_token_consume_tokens
        CHECK (tokens_consumed > 0),
    CONSTRAINT chk_token_consume_balance_after
        CHECK (balance_after >= 0)
);

COMMENT ON TABLE token_consume_logs IS 'AI 算力消耗明细表；余额以 wallet_transactions 为准';
COMMENT ON COLUMN token_consume_logs.id IS '雪花主键';
COMMENT ON COLUMN token_consume_logs.enterprise_id IS '逻辑关联 enterprises.id';
COMMENT ON COLUMN token_consume_logs.user_id IS '逻辑关联 sys_users.id';
COMMENT ON COLUMN token_consume_logs.wallet_transaction_id IS '逻辑关联成功的 CONSUME 流水';

CREATE INDEX IF NOT EXISTS idx_token_consume_enterprise_created
    ON token_consume_logs (enterprise_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_token_consume_business_attempt
    ON token_consume_logs (biz_type, biz_id, attempt_no);

COMMIT;
