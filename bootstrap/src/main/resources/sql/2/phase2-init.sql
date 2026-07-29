-- ============================================================
-- Phase 2: 简历接收与 AI 初筛
-- 数据库: PostgreSQL 14+
-- 说明: 不使用物理外键，所有外键关系在应用层保证
--       时间字段使用 TIMESTAMPTZ，布尔使用 BOOLEAN，JSON 使用 JSONB
--       主键由应用层雪花算法生成，DDL 仅声明 BIGINT NOT NULL
-- ============================================================

BEGIN;

-- ==================== 1. resumes ====================
CREATE TABLE IF NOT EXISTS resumes (
    id              BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    file_name       VARCHAR(256),
    file_size       BIGINT,
    file_type       VARCHAR(16),
    file_hash       VARCHAR(64)     NOT NULL,
    storage_url     TEXT,
    resume_text     TEXT,
    analyze_status  VARCHAR(20),
    upload_deadline_at TIMESTAMPTZ,
    cleanup_message_id BIGINT,
    created_at      TIMESTAMPTZ     NOT NULL,
    is_deleted      BOOLEAN         DEFAULT FALSE,
    updated_by      BIGINT,
    trace_id        VARCHAR(128),
    updated_at      TIMESTAMPTZ,
    PRIMARY KEY (id)
);

COMMENT ON TABLE resumes IS '简历底座表';
COMMENT ON COLUMN resumes.id IS '主键';
COMMENT ON COLUMN resumes.user_id IS '[逻辑外键]→sys_users, 候选人用户 ID';
COMMENT ON COLUMN resumes.file_name IS '原始文件名';
COMMENT ON COLUMN resumes.file_size IS '文件大小（字节）';
COMMENT ON COLUMN resumes.file_type IS 'pdf / doc / docx';
COMMENT ON COLUMN resumes.file_hash IS 'SHA-256 文件哈希';
COMMENT ON COLUMN resumes.storage_url IS 'RustFS / OSS 存储 URL';
COMMENT ON COLUMN resumes.resume_text IS '解析后的简历纯文本';
COMMENT ON COLUMN resumes.analyze_status IS 'UPLOADING / PENDING / PROCESSING / COMPLETED / FAILED / UPLOAD_FAILED';
COMMENT ON COLUMN resumes.upload_deadline_at IS '上传预占截止时间，超时后由修复任务收敛';
COMMENT ON COLUMN resumes.cleanup_message_id IS '上传清理消息逻辑引用，不建立跨模块外键';
COMMENT ON COLUMN resumes.created_at IS '上传时间';
COMMENT ON COLUMN resumes.is_deleted IS '逻辑删除';
COMMENT ON COLUMN resumes.updated_by IS '[逻辑外键]→sys_users';
COMMENT ON COLUMN resumes.trace_id IS '触发解析的调用链 ID';
COMMENT ON COLUMN resumes.updated_at IS '最后更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS idx_resumes_file_hash
    ON resumes (user_id, file_hash) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_resumes_user_id ON resumes (user_id);
CREATE INDEX IF NOT EXISTS idx_resumes_upload_timeout
    ON resumes (upload_deadline_at, id)
    WHERE analyze_status = 'UPLOADING' AND is_deleted = FALSE;


-- ==================== 2. resume_analyses ====================
CREATE TABLE IF NOT EXISTS resume_analyses (
    id              BIGINT          NOT NULL,
    resume_id       BIGINT          NOT NULL,
    overall_score   INT,
    strengths_json  JSONB,
    suggestions_json JSONB,
    llm_config_snapshot JSONB        NOT NULL,
    analyzed_at     TIMESTAMPTZ,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    trace_id        VARCHAR(128),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT chk_resume_analysis_llm_snapshot
        CHECK (jsonb_typeof(llm_config_snapshot) = 'object')
);

COMMENT ON TABLE resume_analyses IS '简历 AI 分析结果表';
COMMENT ON COLUMN resume_analyses.id IS '主键';
COMMENT ON COLUMN resume_analyses.resume_id IS '[逻辑外键]→resumes';
COMMENT ON COLUMN resume_analyses.overall_score IS 'AI 综合评分 (0-100)';
COMMENT ON COLUMN resume_analyses.strengths_json IS '优点列表 (JSON)';
COMMENT ON COLUMN resume_analyses.suggestions_json IS '改进建议 (JSON)';
COMMENT ON COLUMN resume_analyses.llm_config_snapshot IS '任务创建时固化的 LLM 场景、Provider、模型、参数及版本快照，不含密钥';
COMMENT ON COLUMN resume_analyses.analyzed_at IS '分析完成时间；任务待执行或处理中为空';
COMMENT ON COLUMN resume_analyses.is_deleted IS '逻辑删除';
COMMENT ON COLUMN resume_analyses.trace_id IS '调用链 ID（追溯大模型响应）';
COMMENT ON COLUMN resume_analyses.created_at IS '创建时间';

CREATE INDEX IF NOT EXISTS idx_resume_analyses_resume_id ON resume_analyses (resume_id);


-- ==================== 3. candidate_skill_scores ====================
CREATE TABLE IF NOT EXISTS candidate_skill_scores (
    id                  BIGINT          NOT NULL,
    resume_analysis_id  BIGINT          NOT NULL,
    dimension_code      VARCHAR(32)     NOT NULL,
    score               INT             NOT NULL,
    ai_justification    TEXT,
    is_deleted          BOOLEAN         DEFAULT FALSE,
    created_at          TIMESTAMPTZ     NOT NULL,
    PRIMARY KEY (id)
);

COMMENT ON TABLE candidate_skill_scores IS '标准化人才画像维度打分表';
COMMENT ON COLUMN candidate_skill_scores.id IS '主键';
COMMENT ON COLUMN candidate_skill_scores.resume_analysis_id IS '[逻辑外键]→resume_analyses';
COMMENT ON COLUMN candidate_skill_scores.dimension_code IS '打分维度编码';
COMMENT ON COLUMN candidate_skill_scores.score IS '单项得分 (0-100)';
COMMENT ON COLUMN candidate_skill_scores.ai_justification IS '大模型针对该维度给出扣分或得分的推导依据';
COMMENT ON COLUMN candidate_skill_scores.is_deleted IS '逻辑删除';
COMMENT ON COLUMN candidate_skill_scores.created_at IS '创建时间';

CREATE UNIQUE INDEX IF NOT EXISTS idx_skill_scores_unique ON candidate_skill_scores (resume_analysis_id, dimension_code);


-- ==================== 4. candidate_profile ====================
CREATE TABLE IF NOT EXISTS candidate_profile (
    id                  BIGINT          NOT NULL,
    user_id             BIGINT          NOT NULL,
    dimension_code      VARCHAR(32)     NOT NULL,
    avg_score           INT,
    latest_justification TEXT,
    is_deleted          BOOLEAN         DEFAULT FALSE,
    created_at          TIMESTAMPTZ     NOT NULL,
    updated_at          TIMESTAMPTZ,
    PRIMARY KEY (id)
);

COMMENT ON TABLE candidate_profile IS '候选人画像聚合表（供雷达图读取）';
COMMENT ON COLUMN candidate_profile.id IS '主键';
COMMENT ON COLUMN candidate_profile.user_id IS '[逻辑外键]→sys_users, 仅 user_type=''CANDIDATE''';
COMMENT ON COLUMN candidate_profile.dimension_code IS '打分维度编码';
COMMENT ON COLUMN candidate_profile.avg_score IS '各版简历该维度的平均分';
COMMENT ON COLUMN candidate_profile.latest_justification IS '最新简历的 AI 推导依据';
COMMENT ON COLUMN candidate_profile.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN candidate_profile.created_at IS '创建时间';
COMMENT ON COLUMN candidate_profile.updated_at IS '更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS idx_candidate_profile_unique ON candidate_profile (user_id, dimension_code);


-- ==================== 5. job_applications ====================
CREATE TABLE IF NOT EXISTS job_applications (
    id              BIGINT          NOT NULL,
    enterprise_id   BIGINT          NOT NULL,
    job_id          BIGINT          NOT NULL,
    candidate_id    BIGINT          NOT NULL,
    resume_id       BIGINT          NOT NULL,
    idempotency_key VARCHAR(128)    NOT NULL,
    ai_match_score  INT,
    status          VARCHAR(32)     DEFAULT 'APPLIED',
    created_at      TIMESTAMPTZ     NOT NULL,
    is_deleted      BOOLEAN         DEFAULT FALSE,
    updated_by      BIGINT,
    trace_id        VARCHAR(128),
    updated_at      TIMESTAMPTZ,
    PRIMARY KEY (id)
);

COMMENT ON TABLE job_applications IS '简历投递与初筛记录表';
COMMENT ON COLUMN job_applications.id IS '主键';
COMMENT ON COLUMN job_applications.enterprise_id IS '强隔离：关联企业租户 ID';
COMMENT ON COLUMN job_applications.job_id IS '[逻辑外键]→jobs';
COMMENT ON COLUMN job_applications.candidate_id IS '[逻辑外键]→sys_users, 仅 user_type=''CANDIDATE''';
COMMENT ON COLUMN job_applications.resume_id IS '[逻辑外键]→resumes';
COMMENT ON COLUMN job_applications.idempotency_key IS '候选人投递请求幂等键';
COMMENT ON COLUMN job_applications.ai_match_score IS '大模型计算的人岗匹配度打分';
COMMENT ON COLUMN job_applications.status IS 'APPLIED / REVIEWING / PASSED / REJECTED / WITHDRAWN';
COMMENT ON COLUMN job_applications.created_at IS '投递时间';
COMMENT ON COLUMN job_applications.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN job_applications.updated_by IS '轻量审计：操作的 HR ID';
COMMENT ON COLUMN job_applications.trace_id IS '调用链 ID';
COMMENT ON COLUMN job_applications.updated_at IS '状态更新时间';

CREATE INDEX IF NOT EXISTS idx_applications_job_id ON job_applications (job_id);
CREATE INDEX IF NOT EXISTS idx_applications_candidate_id ON job_applications (candidate_id);
CREATE INDEX IF NOT EXISTS idx_applications_status ON job_applications (enterprise_id, status);
-- 唯一索引：同一候选人 + 同一岗位仅允许一条未删除投递记录，防御并发重复提交
CREATE UNIQUE INDEX IF NOT EXISTS uk_applications_job_candidate_active
    ON job_applications (job_id, candidate_id) WHERE is_deleted = false;
CREATE UNIQUE INDEX IF NOT EXISTS uk_applications_candidate_idempotency_active
    ON job_applications (candidate_id, idempotency_key)
    WHERE is_deleted = false;


-- ==================== 6. local_message ====================
CREATE TABLE IF NOT EXISTS local_message (
    id              BIGINT          NOT NULL,
    topic           VARCHAR(64)     NOT NULL,
    biz_key         VARCHAR(160),
    schema_version  SMALLINT        NOT NULL DEFAULT 1,
    payload         JSONB           NOT NULL,
    priority        VARCHAR(16)     NOT NULL DEFAULT 'MEDIUM',
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    retry_count     INT             NOT NULL DEFAULT 0,
    max_retries     INT             NOT NULL DEFAULT 3,
    next_retry_at   TIMESTAMPTZ,
    retry_history   JSONB,
    last_error      TEXT,
    lease_owner     VARCHAR(128),
    lease_until     TIMESTAMPTZ,
    lease_version   BIGINT          NOT NULL DEFAULT 0,
    trace_id        VARCHAR(128),
    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ,
    PRIMARY KEY (id)
);

COMMENT ON TABLE local_message IS '通用本地消息表（异步补偿/重试）';
COMMENT ON COLUMN local_message.id IS '主键';
COMMENT ON COLUMN local_message.topic IS '消息主题；FILE_DELETE 仅保留兼容旧记录';
COMMENT ON COLUMN local_message.biz_key IS '业务幂等键，同一 topic 下唯一';
COMMENT ON COLUMN local_message.schema_version IS '消息载荷协议版本';
COMMENT ON COLUMN local_message.payload IS '业务数据 JSON';
COMMENT ON COLUMN local_message.priority IS '优先级：HIGH / MEDIUM / LOW';
COMMENT ON COLUMN local_message.status IS 'PENDING / PROCESSING / SUCCESS / FAILED / IGNORED';
COMMENT ON COLUMN local_message.retry_count IS '已重试次数';
COMMENT ON COLUMN local_message.max_retries IS '最大重试次数';
COMMENT ON COLUMN local_message.next_retry_at IS '下次重试时间（指数退避）';
COMMENT ON COLUMN local_message.retry_history IS '重试历史数组：[{"retry":1,"at":"...","error":"...","traceId":"..."}]';
COMMENT ON COLUMN local_message.last_error IS '最近一次失败原因';
COMMENT ON COLUMN local_message.lease_owner IS '当前租约持有者';
COMMENT ON COLUMN local_message.lease_until IS '租约截止时间，过期 PROCESSING 消息允许重领';
COMMENT ON COLUMN local_message.lease_version IS '租约栅栏版本，阻止旧执行者回写';
COMMENT ON COLUMN local_message.trace_id IS '触发该消息的调用链 ID';
COMMENT ON COLUMN local_message.created_at IS '创建时间';
COMMENT ON COLUMN local_message.updated_at IS '更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_local_message_topic_biz_key
    ON local_message (topic, biz_key) WHERE biz_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_local_message_dispatch
    ON local_message (priority, status, next_retry_at, lease_until, created_at)
    WHERE status IN ('PENDING', 'PROCESSING');


-- ==================== 7. llm_provider_config ====================
CREATE TABLE IF NOT EXISTS llm_provider_config (
    id                  VARCHAR(64)     NOT NULL,
    base_url            VARCHAR(512)    NOT NULL,
    api_key_ciphertext  TEXT            NOT NULL,
    model               VARCHAR(128)    NOT NULL,
    model_type          VARCHAR(32)     NOT NULL,
    enabled             BOOLEAN         NOT NULL DEFAULT FALSE,
    version             INT             NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    updated_by          BIGINT,
    trace_id            VARCHAR(128),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT chk_llm_provider_version CHECK (version >= 0),
    CONSTRAINT chk_llm_provider_model_type
        CHECK (model_type IN ('CHAT', 'EMBEDDING', 'ASR', 'TTS'))
);

COMMENT ON TABLE llm_provider_config IS '大模型路由密钥表';
COMMENT ON COLUMN llm_provider_config.id IS '可路由配置 ID，如 dashscope-chat、openai-embedding';
COMMENT ON COLUMN llm_provider_config.base_url IS 'API 网关地址';
COMMENT ON COLUMN llm_provider_config.api_key_ciphertext IS 'AES-GCM 密文信封，包含格式版本、密钥 ID、随机 IV、密文和认证标签';
COMMENT ON COLUMN llm_provider_config.model IS '当前路由使用的模型名';
COMMENT ON COLUMN llm_provider_config.model_type IS '模型能力类型：CHAT / EMBEDDING / ASR / TTS';
COMMENT ON COLUMN llm_provider_config.enabled IS '路由开关';
COMMENT ON COLUMN llm_provider_config.version IS '管理端 CAS 版本';
COMMENT ON COLUMN llm_provider_config.created_at IS '创建时间';
COMMENT ON COLUMN llm_provider_config.is_deleted IS '逻辑删除';
COMMENT ON COLUMN llm_provider_config.updated_by IS '[逻辑外键]→sys_users，最后修改人';
COMMENT ON COLUMN llm_provider_config.trace_id IS '最后一次修改的调用链 ID';
COMMENT ON COLUMN llm_provider_config.updated_at IS '最后更新时间';


-- ==================== 8. ai_global_route ====================
CREATE TABLE IF NOT EXISTS ai_global_route (
    model_type     VARCHAR(32)     NOT NULL,
    provider_id    VARCHAR(64),
    version        INT             NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by     BIGINT,
    trace_id       VARCHAR(128),
    updated_at     TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (model_type),
    CONSTRAINT chk_ai_global_route_model_type
        CHECK (model_type IN ('CHAT', 'EMBEDDING', 'ASR', 'TTS')),
    CONSTRAINT chk_ai_global_route_version CHECK (version >= 0)
);

COMMENT ON TABLE ai_global_route IS '按模型能力划分的 AI 全局默认路由表';
COMMENT ON COLUMN ai_global_route.model_type IS '模型能力类型，每种类型只有一个默认路由槽位';
COMMENT ON COLUMN ai_global_route.provider_id IS '[逻辑外键]→llm_provider_config；为空表示尚未配置';
COMMENT ON COLUMN ai_global_route.version IS '该模型类型默认路由的管理端 CAS 版本';
COMMENT ON COLUMN ai_global_route.created_at IS '创建时间';
COMMENT ON COLUMN ai_global_route.updated_by IS '[逻辑外键]→sys_users，最后修改人';
COMMENT ON COLUMN ai_global_route.trace_id IS '最后一次修改的调用链 ID';
COMMENT ON COLUMN ai_global_route.updated_at IS '最后更新时间';

INSERT INTO ai_global_route (model_type, provider_id)
VALUES
    ('CHAT', NULL),
    ('EMBEDDING', NULL),
    ('ASR', NULL),
    ('TTS', NULL)
ON CONFLICT (model_type) DO NOTHING;


-- ==================== 9. llm_scene_config ====================
CREATE TABLE IF NOT EXISTS llm_scene_config (
    scene_code          VARCHAR(64)     NOT NULL,
    model_type          VARCHAR(16)     NOT NULL,
    provider_id         VARCHAR(64),
    temperature         NUMERIC(4,3),
    top_p               NUMERIC(4,3),
    max_input_tokens    INT             NOT NULL,
    max_output_tokens   INT             NOT NULL,
    timeout_seconds     INT             NOT NULL DEFAULT 60,
    prompt_version      VARCHAR(64)     NOT NULL,
    extra_options       JSONB           NOT NULL DEFAULT '{}',
    enabled             BOOLEAN         NOT NULL DEFAULT TRUE,
    version             INT             NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          BIGINT,
    trace_id            VARCHAR(128),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (scene_code),
    CONSTRAINT chk_scene_model_type CHECK (model_type IN ('CHAT', 'EMBEDDING')),
    CONSTRAINT chk_scene_temperature CHECK (temperature IS NULL OR (temperature >= 0 AND temperature <= 2)),
    CONSTRAINT chk_scene_top_p CHECK (top_p IS NULL OR (top_p > 0 AND top_p <= 1)),
    CONSTRAINT chk_scene_max_input_tokens CHECK (max_input_tokens BETWEEN 256 AND 1000000),
    CONSTRAINT chk_scene_max_output_tokens CHECK (max_output_tokens BETWEEN 1 AND 32768),
    CONSTRAINT chk_scene_timeout CHECK (timeout_seconds BETWEEN 5 AND 180),
    CONSTRAINT chk_scene_version CHECK (version >= 0),
    CONSTRAINT chk_scene_extra_options CHECK (jsonb_typeof(extra_options) = 'object')
);

COMMENT ON TABLE llm_scene_config IS 'AI 场景执行参数表';
COMMENT ON COLUMN llm_scene_config.scene_code IS '场景编码，如 RESUME_ANALYSIS、JOB_RESUME_MATCHING';
COMMENT ON COLUMN llm_scene_config.model_type IS 'CHAT / EMBEDDING；决定未指定 Provider 时使用哪类全局默认路由';
COMMENT ON COLUMN llm_scene_config.provider_id IS '[逻辑外键]→llm_provider_config；为空时按 model_type 使用 ai_global_route';
COMMENT ON COLUMN llm_scene_config.temperature IS '采样温度；为空时由模型适配器采用代码默认值或不发送该参数';
COMMENT ON COLUMN llm_scene_config.top_p IS '核采样参数；为空时由模型适配器采用代码默认值或不发送该参数';
COMMENT ON COLUMN llm_scene_config.max_input_tokens IS '输入上下文预算，超出后由场景策略裁剪或拒绝';
COMMENT ON COLUMN llm_scene_config.max_output_tokens IS '最大输出 Token 数；适配器映射为供应商对应字段';
COMMENT ON COLUMN llm_scene_config.timeout_seconds IS '单次模型调用超时，不代表业务任务总超时';
COMMENT ON COLUMN llm_scene_config.prompt_version IS '提示词模板版本，如 resume-analysis-v1';
COMMENT ON COLUMN llm_scene_config.extra_options IS '经服务端白名单校验的供应商特有参数，禁止透传任意客户端参数';
COMMENT ON COLUMN llm_scene_config.enabled IS '场景开关；关闭后拒绝创建新的该场景 AI 任务';
COMMENT ON COLUMN llm_scene_config.version IS '管理端更新及启停使用的 CAS 版本';
COMMENT ON COLUMN llm_scene_config.created_at IS '创建时间';
COMMENT ON COLUMN llm_scene_config.updated_by IS '[逻辑外键]→sys_users，最后修改人';
COMMENT ON COLUMN llm_scene_config.trace_id IS '最后一次修改的调用链 ID';
COMMENT ON COLUMN llm_scene_config.updated_at IS '最后更新时间';

CREATE INDEX IF NOT EXISTS idx_llm_scene_provider ON llm_scene_config (provider_id);
CREATE INDEX IF NOT EXISTS idx_llm_scene_type_enabled ON llm_scene_config (model_type, enabled);

-- 第二阶段仅初始化当前已进入建设范围的场景；后续阶段在各自脚本中追加场景。
INSERT INTO llm_scene_config (
    scene_code,
    model_type,
    provider_id,
    temperature,
    top_p,
    max_input_tokens,
    max_output_tokens,
    timeout_seconds,
    prompt_version,
    extra_options,
    enabled,
    version,
    created_at,
    updated_at
) VALUES
(
    'RESUME_ANALYSIS',
    'CHAT',
    NULL,
    0.200,
    0.900,
    16000,
    2000,
    60,
    'resume-analysis-v1',
    '{}'::jsonb,
    TRUE,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'JOB_RESUME_MATCHING',
    'CHAT',
    NULL,
    0.200,
    0.900,
    16000,
    1000,
    60,
    'job-resume-matching-v1',
    '{}'::jsonb,
    TRUE,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (scene_code) DO NOTHING;

COMMIT;
