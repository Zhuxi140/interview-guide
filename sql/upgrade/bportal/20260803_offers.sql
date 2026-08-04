BEGIN;

CREATE TABLE IF NOT EXISTS offers (
    id                          BIGINT          NOT NULL,
    enterprise_id               BIGINT          NOT NULL,
    application_id              BIGINT          NOT NULL,
    candidate_user_id           BIGINT          NOT NULL,
    title                       VARCHAR(128)    NOT NULL,
    salary_min                  NUMERIC(12, 2),
    salary_max                  NUMERIC(12, 2),
    currency                    VARCHAR(3)      NOT NULL DEFAULT 'CNY',
    planned_start_date          DATE,
    content                     TEXT            NOT NULL,
    expires_at                  TIMESTAMPTZ     NOT NULL,
    status                      VARCHAR(16)     NOT NULL DEFAULT 'DRAFT',
    create_idempotency_key      VARCHAR(128)    NOT NULL,
    send_idempotency_key        VARCHAR(128),
    decision_idempotency_key    VARCHAR(128),
    decision_reason             VARCHAR(256),
    withdraw_reason             VARCHAR(256),
    sent_at                     TIMESTAMPTZ,
    decided_at                  TIMESTAMPTZ,
    withdrawn_at                TIMESTAMPTZ,
    version                     INT             NOT NULL DEFAULT 0,
    created_by                  BIGINT          NOT NULL,
    updated_by                  BIGINT,
    trace_id                    VARCHAR(128),
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_offers_status
        CHECK (status IN ('DRAFT', 'SENT', 'ACCEPTED', 'DECLINED', 'WITHDRAWN', 'EXPIRED')),
    CONSTRAINT ck_offers_salary
        CHECK (
            (salary_min IS NULL AND salary_max IS NULL)
            OR (
                salary_min IS NOT NULL
                AND salary_max IS NOT NULL
                AND salary_min >= 0
                AND salary_max >= salary_min
            )
        ),
    CONSTRAINT ck_offers_currency
        CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_offers_title
        CHECK (btrim(title) <> ''),
    CONSTRAINT ck_offers_content
        CHECK (btrim(content) <> ''),
    CONSTRAINT ck_offers_expiration
        CHECK (expires_at > created_at),
    CONSTRAINT ck_offers_version
        CHECK (version >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_offers_application_open_or_accepted
    ON offers (application_id)
    WHERE status IN ('DRAFT', 'SENT', 'ACCEPTED');
CREATE UNIQUE INDEX IF NOT EXISTS uk_offers_create_idempotency
    ON offers (enterprise_id, created_by, create_idempotency_key);
CREATE UNIQUE INDEX IF NOT EXISTS uk_offers_send_idempotency
    ON offers (enterprise_id, send_idempotency_key)
    WHERE send_idempotency_key IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_offers_decision_idempotency
    ON offers (candidate_user_id, decision_idempotency_key)
    WHERE decision_idempotency_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_offers_application_created
    ON offers (application_id, created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_offers_candidate_status_created
    ON offers (candidate_user_id, status, created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_offers_enterprise_status_created
    ON offers (enterprise_id, status, created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_offers_pending_expiration
    ON offers (expires_at, id)
    WHERE status = 'SENT';

COMMENT ON TABLE offers IS '企业向候选人发出的录用邀请及其决策状态';
COMMENT ON COLUMN offers.enterprise_id IS '[逻辑外键]→enterprises';
COMMENT ON COLUMN offers.application_id IS '[逻辑外键]→job_applications';
COMMENT ON COLUMN offers.candidate_user_id IS '[逻辑外键]→sys_users，冗余保存以支持候选人归属校验和查询';
COMMENT ON COLUMN offers.status IS 'DRAFT / SENT / ACCEPTED / DECLINED / WITHDRAWN / EXPIRED';
COMMENT ON COLUMN offers.version IS '修改、发送和撤回使用的乐观锁版本号';
COMMENT ON COLUMN offers.created_by IS '[逻辑外键]→sys_users，创建 Offer 的企业用户';

COMMIT;

