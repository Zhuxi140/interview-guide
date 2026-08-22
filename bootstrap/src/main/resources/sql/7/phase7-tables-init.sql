-- 第七阶段：企业知识库与 RAG 表结构初始化
-- PostgreSQL；跨模块关联仅使用逻辑外键，不创建物理 FOREIGN KEY。
-- 依据 docs/第7阶段/第七阶段数据库表设计.md 与 docs/databases.md 生成。
--
-- 说明：
-- 1. document_chunks 向量切片表本阶段不创建：
--    embedding_vector 依赖 pgvector 扩展（CREATE EXTENSION vector），当前环境未启用；
--    简单接口的切片数量通过 knowledge_bases.chunk_count 冗余列返回。
--    TODO: pgvector 扩展启用后补充 document_chunks 建表（UUID 主键 + VECTOR(1024) + JSONB 元数据）。
-- 2. rag_chat_sessions 在表设计文档基础上增加 enterprise_id 租户隔离列：
--    接口契约要求"sessionId 必须同时属于路径中的企业"，且会话创建响应需返回 enterpriseId。
-- 3. 关联表（rag_session_knowledge_bases / interview_template_knowledge_bases）
--    采用雪花 id 主键 + 部分唯一索引（WHERE is_deleted = FALSE），
--    支持逻辑删除后的重复绑定；模板-知识库绑定为第七阶段新增关联表
--    （表设计中无独立表，不改既有 interview_stage_templates 结构）。

BEGIN;

-- 知识库文档表
CREATE TABLE IF NOT EXISTS knowledge_bases
(
    id            BIGINT       PRIMARY KEY,
    visibility    VARCHAR(32)  NOT NULL DEFAULT 'PRIVATE',
    enterprise_id BIGINT       NOT NULL DEFAULT 0,
    file_hash     VARCHAR(64)  NOT NULL,
    file_name     VARCHAR(256),
    file_size     BIGINT,
    name          VARCHAR(256) NOT NULL,
    vector_status VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    vector_error  VARCHAR(512),
    chunk_count   INT          NOT NULL DEFAULT 0,
    version       INT          NOT NULL DEFAULT 0,
    uploaded_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_by    BIGINT,
    trace_id      VARCHAR(128),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_knowledge_bases_visibility
        CHECK (visibility IN ('GLOBAL', 'PRIVATE')),
    CONSTRAINT chk_knowledge_bases_vector_status
        CHECK (vector_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_knowledge_bases_tenant
        CHECK (
            (visibility = 'GLOBAL' AND enterprise_id = 0)
            OR (visibility = 'PRIVATE' AND enterprise_id <> 0)
        ),
    CONSTRAINT chk_knowledge_bases_version
        CHECK (version >= 0),
    CONSTRAINT chk_knowledge_bases_chunk_count
        CHECK (chunk_count >= 0)
);

COMMENT ON TABLE knowledge_bases IS '知识库文档表；GLOBAL 为平台公共底座（enterprise_id=0），PRIVATE 为企业私有文档';
COMMENT ON COLUMN knowledge_bases.id IS '雪花主键';
COMMENT ON COLUMN knowledge_bases.visibility IS 'GLOBAL (公共底座) / PRIVATE (企业私有)';
COMMENT ON COLUMN knowledge_bases.enterprise_id IS '逻辑关联 enterprises.id，GLOBAL 级别为 0';
COMMENT ON COLUMN knowledge_bases.file_hash IS 'SHA-256 文件哈希，按 (enterprise_id, file_hash) 防重';
COMMENT ON COLUMN knowledge_bases.vector_status IS '向量化状态 PENDING/PROCESSING/COMPLETED/FAILED';
COMMENT ON COLUMN knowledge_bases.vector_error IS '最近一次向量化失败原因，成功后清空';
COMMENT ON COLUMN knowledge_bases.chunk_count IS '向量分块数量冗余列（document_chunks 计数）';
COMMENT ON COLUMN knowledge_bases.version IS '乐观锁版本号，管理端删除防并发覆盖';
COMMENT ON COLUMN knowledge_bases.uploaded_at IS '上传时间';

-- 逻辑删除后允许同租户重新上传同哈希文件，故唯一约束挂在未删除行上
CREATE UNIQUE INDEX IF NOT EXISTS uk_knowledge_bases_enterprise_hash
    ON knowledge_bases (enterprise_id, file_hash)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_knowledge_bases_enterprise
    ON knowledge_bases (enterprise_id, id DESC)
    WHERE is_deleted = FALSE AND visibility = 'PRIVATE';

CREATE INDEX IF NOT EXISTS idx_knowledge_bases_global
    ON knowledge_bases (id DESC)
    WHERE is_deleted = FALSE AND visibility = 'GLOBAL';

-- RAG 对话会话主表
CREATE TABLE IF NOT EXISTS rag_chat_sessions
(
    id            BIGINT       PRIMARY KEY,
    enterprise_id BIGINT       NOT NULL,
    user_id       BIGINT       NOT NULL,
    title         VARCHAR(256),
    is_deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    trace_id      VARCHAR(128),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE rag_chat_sessions IS 'RAG 对话会话主表；会话仅对创建者本人可见，enterprise_id 为租户隔离键';
COMMENT ON COLUMN rag_chat_sessions.id IS '雪花主键';
COMMENT ON COLUMN rag_chat_sessions.enterprise_id IS '逻辑关联 enterprises.id，会话归属企业';
COMMENT ON COLUMN rag_chat_sessions.user_id IS '逻辑关联 sys_users.id，创建者（企业成员）';

CREATE INDEX IF NOT EXISTS idx_rag_chat_sessions_user
    ON rag_chat_sessions (enterprise_id, user_id, id DESC)
    WHERE is_deleted = FALSE;

-- RAG 对话消息流水表
-- 为保证写入性能仅配置 trace_id 与 is_deleted，抛弃厚重审计。
CREATE TABLE IF NOT EXISTS rag_chat_messages
(
    id         BIGINT       PRIMARY KEY,
    session_id BIGINT       NOT NULL,
    type       VARCHAR(16)  NOT NULL,
    content    TEXT         NOT NULL,
    is_deleted BOOLEAN      NOT NULL DEFAULT FALSE,
    trace_id   VARCHAR(128),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_rag_chat_messages_type
        CHECK (type IN ('USER', 'AI'))
);

COMMENT ON TABLE rag_chat_messages IS 'RAG 对话消息流水表；type=USER/AI，回答状态与引用溯源由异步回答流程回填';
COMMENT ON COLUMN rag_chat_messages.id IS '雪花主键，同时作为游标分页游标';
COMMENT ON COLUMN rag_chat_messages.session_id IS '逻辑关联 rag_chat_sessions.id';

CREATE INDEX IF NOT EXISTS idx_rag_chat_messages_session
    ON rag_chat_messages (session_id, id)
    WHERE is_deleted = FALSE;

-- RAG 会话-知识库关联表
CREATE TABLE IF NOT EXISTS rag_session_knowledge_bases
(
    id                BIGINT      PRIMARY KEY,
    session_id        BIGINT      NOT NULL,
    knowledge_base_id BIGINT      NOT NULL,
    is_deleted        BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trace_id          VARCHAR(128)
);

COMMENT ON TABLE rag_session_knowledge_bases IS 'RAG 会话-知识库关联表；完整替换语义（删旧插新）';
COMMENT ON COLUMN rag_session_knowledge_bases.id IS '雪花主键';
COMMENT ON COLUMN rag_session_knowledge_bases.session_id IS '逻辑关联 rag_chat_sessions.id';
COMMENT ON COLUMN rag_session_knowledge_bases.knowledge_base_id IS '逻辑关联 knowledge_bases.id';

-- 未删除行上保证同一会话不重复绑定同一知识库；逻辑删除后可重新绑定
CREATE UNIQUE INDEX IF NOT EXISTS uk_rag_session_knowledge_bases
    ON rag_session_knowledge_bases (session_id, knowledge_base_id)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_rag_session_knowledge_bases_kb
    ON rag_session_knowledge_bases (knowledge_base_id)
    WHERE is_deleted = FALSE;

-- 面试模板-知识库关联表（第七阶段新增）
-- 表设计无独立模板绑定表，且不修改既有 interview_stage_templates 结构，故新建关联表存储绑定集合。
CREATE TABLE IF NOT EXISTS interview_template_knowledge_bases
(
    id                BIGINT      PRIMARY KEY,
    template_id       BIGINT      NOT NULL,
    knowledge_base_id BIGINT      NOT NULL,
    is_deleted        BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trace_id          VARCHAR(128)
);

COMMENT ON TABLE interview_template_knowledge_bases IS '面试模板-知识库关联表；配置模板允许检索的知识库集合，完整替换语义（删旧插新）';
COMMENT ON COLUMN interview_template_knowledge_bases.id IS '雪花主键';
COMMENT ON COLUMN interview_template_knowledge_bases.template_id IS '逻辑关联 interview_stage_templates.id';
COMMENT ON COLUMN interview_template_knowledge_bases.knowledge_base_id IS '逻辑关联 knowledge_bases.id';

CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_template_knowledge_bases
    ON interview_template_knowledge_bases (template_id, knowledge_base_id)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_interview_template_knowledge_bases_kb
    ON interview_template_knowledge_bases (knowledge_base_id)
    WHERE is_deleted = FALSE;

COMMIT;
