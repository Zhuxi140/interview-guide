-- ============================================================
-- 智能 SaaS 招聘与多模态面试平台 —— 全量 DDL + 权限种子
-- 数据库: PostgreSQL 14+
-- 说明: 不使用物理外键，所有外键关系在应用层保证
--       时间字段使用 TIMESTAMPTZ，布尔使用 BOOLEAN，JSON 使用 JSONB
--       主键由应用层雪花算法生成，DDL 仅声明 BIGINT NOT NULL
-- ============================================================

-- 创建数据库（首次执行时取消注释）
-- CREATE DATABASE interview_guide
--     WITH ENCODING 'UTF8'
--     LC_COLLATE = 'en_US.UTF-8'
--     LC_CTYPE = 'en_US.UTF-8';
-- \c interview_guide

-- ==================== 1. sys_users ====================
CREATE TABLE IF NOT EXISTS sys_users (
    id              BIGINT          NOT NULL,
    username        VARCHAR(64)     NOT NULL,
    email           VARCHAR(128)    UNIQUE,
    password_hash   VARCHAR(128)    NOT NULL,
    nickname        VARCHAR(128),
    avatar_url      VARCHAR(512),
    phone           VARCHAR(64),
    user_type       VARCHAR(16)     NOT NULL,
    risk_level      SMALLINT        DEFAULT 0,
    status          SMALLINT        NOT NULL DEFAULT 2,
    version         INT             NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN         DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ,
    PRIMARY KEY (id)
);

COMMENT ON TABLE sys_users IS '系统用户主表（核心用户表，多张表依赖它）';
COMMENT ON COLUMN sys_users.id IS '主键，采用雪花算法生成（作为多租户全局隔离键）';
COMMENT ON COLUMN sys_users.username IS '登录账号';
COMMENT ON COLUMN sys_users.email IS '邮箱';
COMMENT ON COLUMN sys_users.password_hash IS 'BCrypt 加密后的密码摘要';
COMMENT ON COLUMN sys_users.nickname IS '用户昵称';
COMMENT ON COLUMN sys_users.avatar_url IS '头像存储路径 / RustFS 存储 URL';
COMMENT ON COLUMN sys_users.phone IS '手机号（脱敏存储）';
COMMENT ON COLUMN sys_users.user_type IS '用户类型 (PLATFORM_ADMIN / PLATFORM_OPS / ENTERPRISE_USER / CANDIDATE)';
COMMENT ON COLUMN sys_users.risk_level IS '风控等级 (0: 无风险, 1: 低风险, 2: 中风险, 3: 高风险)';
COMMENT ON COLUMN sys_users.status IS '账号全局状态 (1: 正常, 0: 禁用)';
COMMENT ON COLUMN sys_users.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN sys_users.created_at IS '创建时间';
COMMENT ON COLUMN sys_users.updated_at IS '更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS idx_user_username ON sys_users (username);
CREATE INDEX IF NOT EXISTS idx_user_risk_status ON sys_users (risk_level, status);


-- ==================== 2. user_tokens ====================
CREATE TABLE IF NOT EXISTS user_tokens (
    id                  BIGINT          NOT NULL,
    user_id             BIGINT          NOT NULL,
    refresh_token_hash  VARCHAR(256)    NOT NULL,
    device_info         VARCHAR(256),
    ip_address          VARCHAR(45),
    expires_at          TIMESTAMPTZ     NOT NULL,
    is_revoked          BOOLEAN         DEFAULT FALSE,
    is_deleted          BOOLEAN         DEFAULT FALSE,
    trace_id            VARCHAR(128),
    created_at          TIMESTAMPTZ     NOT NULL,
    PRIMARY KEY (id)
);

COMMENT ON TABLE user_tokens IS '用户长时 Refresh Token 存储表';
COMMENT ON COLUMN user_tokens.id IS '主键';
COMMENT ON COLUMN user_tokens.user_id IS '关联用户 ID';
COMMENT ON COLUMN user_tokens.refresh_token_hash IS '长时令牌 SHA-256 哈希';
COMMENT ON COLUMN user_tokens.device_info IS '设备信息 / UA';
COMMENT ON COLUMN user_tokens.ip_address IS '登录 IP';
COMMENT ON COLUMN user_tokens.expires_at IS '过期时间';
COMMENT ON COLUMN user_tokens.is_revoked IS '是否手动撤销（踢下线）';
COMMENT ON COLUMN user_tokens.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN user_tokens.trace_id IS '调用链 ID';
COMMENT ON COLUMN user_tokens.created_at IS '创建时间';

CREATE INDEX IF NOT EXISTS idx_token_user_id ON user_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_token_hash ON user_tokens (refresh_token_hash);


-- ==================== 3. sys_permissions ====================
CREATE TABLE IF NOT EXISTS sys_permissions (
    id          BIGINT          NOT NULL,
    perm_code   VARCHAR(64)     NOT NULL,
    perm_type   VARCHAR(16)     NOT NULL,
    api_path    VARCHAR(256),
    status      SMALLINT        DEFAULT 1,
    updated_by  BIGINT,
    trace_id    VARCHAR(128),
    updated_at  TIMESTAMPTZ,
    PRIMARY KEY (id)
);

COMMENT ON TABLE sys_permissions IS '系统 API 资源表';
COMMENT ON COLUMN sys_permissions.id IS '主键';
COMMENT ON COLUMN sys_permissions.perm_code IS '权限标识符 (如 ai:interview:start)';
COMMENT ON COLUMN sys_permissions.perm_type IS '资源类型 (MENU / BUTTON / API)';
COMMENT ON COLUMN sys_permissions.api_path IS '后端接口路径正则';
COMMENT ON COLUMN sys_permissions.status IS '接口状态 (1: 正常, 0: 废弃)';
COMMENT ON COLUMN sys_permissions.updated_by IS '操作人 ID';
COMMENT ON COLUMN sys_permissions.trace_id IS '调用链 ID';
COMMENT ON COLUMN sys_permissions.updated_at IS '更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS idx_perm_code ON sys_permissions (perm_code);


-- ==================== 4. sys_roles ====================
CREATE TABLE IF NOT EXISTS sys_roles (
    id          INTEGER         NOT NULL,
    role_code   VARCHAR(32)     NOT NULL,
    role_name   VARCHAR(64)     NOT NULL,
    role_scope  VARCHAR(16)     NOT NULL,
    updated_by  BIGINT,
    trace_id    VARCHAR(128),
    updated_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ     NOT NULL,
    PRIMARY KEY (id)
);

COMMENT ON TABLE sys_roles IS '系统角色表';
COMMENT ON COLUMN sys_roles.id IS '主键';
COMMENT ON COLUMN sys_roles.role_code IS '角色编码 (如 SUPER_ADMIN, HR_MANAGER, CANDIDATE)';
COMMENT ON COLUMN sys_roles.role_name IS '角色名称描述';
COMMENT ON COLUMN sys_roles.role_scope IS '角色作用域 (PLATFORM / ENTERPRISE / USER)';
COMMENT ON COLUMN sys_roles.updated_by IS '操作人 ID';
COMMENT ON COLUMN sys_roles.trace_id IS '调用链 ID';
COMMENT ON COLUMN sys_roles.updated_at IS '更新时间';
COMMENT ON COLUMN sys_roles.created_at IS '创建时间';

CREATE UNIQUE INDEX IF NOT EXISTS idx_role_code ON sys_roles (role_code);


-- ==================== 5. sys_user_roles ====================
CREATE TABLE IF NOT EXISTS sys_user_roles (
    user_id     BIGINT          NOT NULL,
    role_id     INTEGER         NOT NULL,
    updated_by  BIGINT,
    trace_id    VARCHAR(128),
    updated_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ     NOT NULL,
    PRIMARY KEY (user_id,role_id)
);

COMMENT ON TABLE sys_user_roles IS '用户角色关联表';
COMMENT ON COLUMN sys_user_roles.user_id IS '关联用户 ID';
COMMENT ON COLUMN sys_user_roles.role_id IS '关联角色 ID';
COMMENT ON COLUMN sys_user_roles.updated_by IS '操作人 ID';
COMMENT ON COLUMN sys_user_roles.trace_id IS '调用链 ID';
COMMENT ON COLUMN sys_user_roles.updated_at IS '更新时间';
COMMENT ON COLUMN sys_user_roles.created_at IS '创建时间';

-- ==================== 6. sys_role_permissions ====================
CREATE TABLE IF NOT EXISTS sys_role_permissions (
    role_id         INTEGER         NOT NULL,
    permission_id   BIGINT          NOT NULL,
    updated_by      BIGINT,
    trace_id        VARCHAR(128),
    updated_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL,
    PRIMARY KEY (role_id,permission_id)
);

COMMENT ON TABLE sys_role_permissions IS '角色权限关联表';
COMMENT ON COLUMN sys_role_permissions.role_id IS '关联角色 ID';
COMMENT ON COLUMN sys_role_permissions.permission_id IS '关联权限资源 ID';
COMMENT ON COLUMN sys_role_permissions.updated_by IS '操作人 ID';
COMMENT ON COLUMN sys_role_permissions.trace_id IS '调用链 ID';
COMMENT ON COLUMN sys_role_permissions.updated_at IS '更新时间';
COMMENT ON COLUMN sys_role_permissions.created_at IS '创建时间';

-- ==================== 7. enterprises ====================
CREATE TABLE IF NOT EXISTS enterprises (
    id              BIGINT          NOT NULL,
    name            VARCHAR(128)    NOT NULL,
    short_name      VARCHAR(64)     NOT NULL,
    industry        VARCHAR(64)     NOT NULL,
    scale           VARCHAR(32),
    contact_email   VARCHAR(128)    NOT NULL,
    contact_phone   VARCHAR(20)     NOT NULL,
    status          SMALLINT        NOT NULL    DEFAULT 2,
    logo_url        VARCHAR(512),
    created_at      TIMESTAMPTZ     NOT NULL,
    is_deleted      BOOLEAN         DEFAULT FALSE,
    trace_id        VARCHAR(128),
    updated_at      TIMESTAMPTZ,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_enterprises_name_active ON enterprises (name) WHERE is_deleted = false;

COMMENT ON TABLE enterprises IS '企业租户主表 (SaaS 隔离核心，被约 20 张表依赖)';
COMMENT ON COLUMN enterprises.id IS '企业租户唯一 ID（全局 enterprise_id 隔离键）';
COMMENT ON COLUMN enterprises.name IS '企业法定/注册全称';
COMMENT ON COLUMN enterprises.short_name IS '企业商用简称 / 品牌名';
COMMENT ON COLUMN enterprises.industry IS '所属行业分类';
COMMENT ON COLUMN enterprises.scale IS '企业规模';
COMMENT ON COLUMN enterprises.contact_email IS '企业联系邮箱';
COMMENT ON COLUMN enterprises.contact_phone IS '企业联系电话';
COMMENT ON COLUMN enterprises.status IS '企业状态 (2: 待认证， 1: 正常, 0: 暂停, -1: 注销)';
COMMENT ON COLUMN enterprises.logo_url IS '企业 Logo 存储路径';
COMMENT ON COLUMN enterprises.created_at IS '入驻时间';
COMMENT ON COLUMN enterprises.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN enterprises.trace_id IS '创建时的调用链 ID';
COMMENT ON COLUMN enterprises.updated_at IS '更新时间';


-- ==================== 8. enterprise_team_members ====================
CREATE TABLE IF NOT EXISTS enterprise_team_members (
    id              BIGINT          NOT NULL,
    enterprise_id   BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    role_id         INTEGER         NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL,
    is_deleted      BOOLEAN         DEFAULT FALSE,
    updated_by      BIGINT,
    trace_id        VARCHAR(128),
    updated_at      TIMESTAMPTZ,
    PRIMARY KEY (id)
);

COMMENT ON TABLE enterprise_team_members IS '企业内部团队成员表（角色通过 role_id 关联 sys_roles）';
COMMENT ON COLUMN enterprise_team_members.id IS '主键';
COMMENT ON COLUMN enterprise_team_members.enterprise_id IS '关联的企业租户 ID';
COMMENT ON COLUMN enterprise_team_members.user_id IS '关联的系统用户 ID';
COMMENT ON COLUMN enterprise_team_members.role_id IS '[逻辑外键]→sys_roles，企业内角色';
COMMENT ON COLUMN enterprise_team_members.created_at IS '加入时间';
COMMENT ON COLUMN enterprise_team_members.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN enterprise_team_members.updated_by IS '[逻辑外键]→sys_users，操作人';
COMMENT ON COLUMN enterprise_team_members.trace_id IS '调用链 ID';
COMMENT ON COLUMN enterprise_team_members.updated_at IS '更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS idx_team_member_role ON enterprise_team_members (enterprise_id, user_id, role_id);


-- ==================== 9. jobs ====================
CREATE TABLE IF NOT EXISTS jobs (
    id              BIGINT          NOT NULL,
    enterprise_id   BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    title           VARCHAR(128)    NOT NULL,
    jd_content      TEXT            NOT NULL,
    department      VARCHAR(64)     NOT NULL,
    location        VARCHAR(64)     NOT NULL,
    min_salary      DECIMAL(10, 2),
    max_salary      DECIMAL(10, 2),
    experience_req  SMALLINT,
    education_req   SMALLINT,
    skills_json     JSONB,
    status          SMALLINT        NOT NULL DEFAULT 2,
    version         INT             NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL,
    is_deleted      BOOLEAN         DEFAULT FALSE,
    updated_by      BIGINT,
    trace_id        VARCHAR(128),
    updated_at      TIMESTAMPTZ,
    PRIMARY KEY (id)
);

COMMENT ON TABLE jobs IS '企业招聘岗位表';
COMMENT ON COLUMN jobs.id IS '主键';
COMMENT ON COLUMN jobs.enterprise_id IS '强隔离：关联企业租户 ID';
COMMENT ON COLUMN jobs.user_id IS '归属的 B 端 HR 用户 ID';
COMMENT ON COLUMN jobs.title IS '岗位名称';
COMMENT ON COLUMN jobs.jd_content IS '详细的职位描述与要求';
COMMENT ON COLUMN jobs.department IS '所属部门';
COMMENT ON COLUMN jobs.location IS '工作地点';
COMMENT ON COLUMN jobs.min_salary IS '最低薪资（元）';
COMMENT ON COLUMN jobs.max_salary IS '最高薪资（元）';
COMMENT ON COLUMN jobs.experience_req IS '经验要求枚举编码';
COMMENT ON COLUMN jobs.education_req IS '学历要求枚举编码';
COMMENT ON COLUMN jobs.skills_json IS '技能标签 JSON 数组';
COMMENT ON COLUMN jobs.status IS '岗位状态 (2: 草稿, 1: 开放中, 0: 已关闭)';
COMMENT ON COLUMN jobs.version IS '管理端编辑使用的乐观锁版本号';
COMMENT ON COLUMN jobs.created_at IS '发布时间';
COMMENT ON COLUMN jobs.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN jobs.updated_by IS '轻量审计：操作人';
COMMENT ON COLUMN jobs.trace_id IS '调用链 ID';
COMMENT ON COLUMN jobs.updated_at IS '更新时间';

CREATE INDEX IF NOT EXISTS idx_jobs_enterprise_status ON jobs (enterprise_id, status);


-- ==================== Phase 2: 简历接收与 AI 初筛 ====================


-- ==================== 10. resumes ====================
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
    analysis_message_id BIGINT,
    analysis_idempotency_key_hash VARCHAR(64),
    analysis_attempt_count INT NOT NULL DEFAULT 0,
    analysis_deadline_at TIMESTAMPTZ,
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
COMMENT ON COLUMN resumes.analysis_message_id IS '当前简历 AI 分析消息 ID，同时作为对外 taskId，不建立跨模块外键';
COMMENT ON COLUMN resumes.analysis_idempotency_key_hash IS '当前简历 AI 分析请求幂等键 SHA-256 摘要';
COMMENT ON COLUMN resumes.analysis_attempt_count IS '当前简历 AI 分析已领取次数';
COMMENT ON COLUMN resumes.analysis_deadline_at IS '当前简历 AI 分析执行截止时间';
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
CREATE UNIQUE INDEX IF NOT EXISTS uk_resumes_analysis_message
    ON resumes (analysis_message_id)
    WHERE analysis_message_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_resumes_analysis_timeout
    ON resumes (analysis_deadline_at, id)
    WHERE analyze_status = 'PROCESSING' AND is_deleted = FALSE;

-- ==================== 11. resume_analyses ====================
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
COMMENT ON COLUMN resume_analyses.id IS '分析消息 ID，同时作为对外 taskId';
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


-- ==================== 12. candidate_skill_scores ====================
CREATE TABLE IF NOT EXISTS candidate_skill_scores (
    id                  BIGINT          NOT NULL,
    candidate_profile_id BIGINT         NOT NULL,
    dimension_code      VARCHAR(32)     NOT NULL,
    score               INT             NOT NULL,
    ai_justification    TEXT,
    evidence_json       JSONB           NOT NULL DEFAULT '[]',
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT chk_candidate_skill_score CHECK (score BETWEEN 0 AND 100),
    CONSTRAINT chk_candidate_skill_evidence CHECK (jsonb_typeof(evidence_json) = 'array')
);

COMMENT ON TABLE candidate_skill_scores IS '标准化人才画像维度打分表';
COMMENT ON COLUMN candidate_skill_scores.id IS '主键';
COMMENT ON COLUMN candidate_skill_scores.candidate_profile_id IS '[逻辑外键]→candidate_ai_profiles';
COMMENT ON COLUMN candidate_skill_scores.dimension_code IS '打分维度编码';
COMMENT ON COLUMN candidate_skill_scores.score IS '单项得分 (0-100)';
COMMENT ON COLUMN candidate_skill_scores.ai_justification IS '大模型针对该维度给出扣分或得分的推导依据';
COMMENT ON COLUMN candidate_skill_scores.evidence_json IS '来自简历的评分证据列表';
COMMENT ON COLUMN candidate_skill_scores.is_deleted IS '逻辑删除';
COMMENT ON COLUMN candidate_skill_scores.created_at IS '创建时间';

CREATE UNIQUE INDEX IF NOT EXISTS idx_skill_scores_unique
    ON candidate_skill_scores (candidate_profile_id, dimension_code);


-- ==================== 13. candidate_ai_profiles ====================
CREATE TABLE IF NOT EXISTS candidate_ai_profiles (
    id                      BIGINT          NOT NULL,
    candidate_id            BIGINT          NOT NULL,
    resume_id               BIGINT          NOT NULL,
    source_application_id   BIGINT,
    source_enterprise_id    BIGINT,
    status                  VARCHAR(20)     NOT NULL,
    profile_schema_version  VARCHAR(32)     NOT NULL,
    summary_json            JSONB,
    llm_config_snapshot     JSONB,
    attempt_count           INT             NOT NULL DEFAULT 0,
    deadline_at             TIMESTAMPTZ,
    failure_reason          TEXT,
    analyzed_at             TIMESTAMPTZ,
    is_deleted              BOOLEAN         NOT NULL DEFAULT FALSE,
    created_by              BIGINT,
    trace_id                VARCHAR(128),
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT chk_candidate_ai_profile_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_candidate_ai_profile_attempt CHECK (attempt_count >= 0),
    CONSTRAINT chk_candidate_ai_profile_summary
        CHECK (summary_json IS NULL OR jsonb_typeof(summary_json) = 'object'),
    CONSTRAINT chk_candidate_ai_profile_llm_snapshot
        CHECK (llm_config_snapshot IS NULL OR jsonb_typeof(llm_config_snapshot) = 'object')
);

COMMENT ON TABLE candidate_ai_profiles IS '一份简历的一版岗位无关 AI 人才画像';
COMMENT ON COLUMN candidate_ai_profiles.id IS '画像消息 ID，同时作为 taskId';
COMMENT ON COLUMN candidate_ai_profiles.source_application_id IS '首次触发画像的投递 ID';
COMMENT ON COLUMN candidate_ai_profiles.source_enterprise_id IS '首次承担画像生成的企业 ID；候选人触发时为空';
COMMENT ON COLUMN candidate_ai_profiles.profile_schema_version IS '画像维度和评分规范版本';
COMMENT ON COLUMN candidate_ai_profiles.llm_config_snapshot IS '实际使用的模型和提示词配置快照，不含密钥';

CREATE INDEX IF NOT EXISTS idx_candidate_ai_profiles_candidate
    ON candidate_ai_profiles (candidate_id, created_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uk_candidate_ai_profiles_completed
    ON candidate_ai_profiles (resume_id, profile_schema_version)
    WHERE status = 'COMPLETED' AND is_deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uk_candidate_ai_profiles_active
    ON candidate_ai_profiles (resume_id, profile_schema_version)
    WHERE status IN ('PENDING', 'PROCESSING') AND is_deleted = FALSE;


-- ==================== 14. job_applications ====================
CREATE TABLE IF NOT EXISTS job_applications (
    id              BIGINT          NOT NULL,
    enterprise_id   BIGINT          NOT NULL,
    job_id          BIGINT          NOT NULL,
    candidate_id    BIGINT          NOT NULL,
    resume_id       BIGINT          NOT NULL,
    idempotency_key VARCHAR(128)    NOT NULL,
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
COMMENT ON COLUMN job_applications.status IS 'APPLIED / REVIEWING / PASSED / REJECTED / WITHDRAWN';
COMMENT ON COLUMN job_applications.created_at IS '投递时间';
COMMENT ON COLUMN job_applications.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN job_applications.updated_by IS '轻量审计：操作的 HR ID';
COMMENT ON COLUMN job_applications.trace_id IS '调用链 ID';
COMMENT ON COLUMN job_applications.updated_at IS '状态更新时间';

CREATE INDEX IF NOT EXISTS idx_applications_job_id ON job_applications (job_id);
CREATE INDEX IF NOT EXISTS idx_applications_candidate_id ON job_applications (candidate_id);
CREATE INDEX IF NOT EXISTS idx_applications_status ON job_applications (enterprise_id, status);
CREATE UNIQUE INDEX IF NOT EXISTS uk_applications_job_candidate_active
    ON job_applications (job_id, candidate_id) WHERE is_deleted = false;
CREATE UNIQUE INDEX IF NOT EXISTS uk_applications_candidate_idempotency_active
    ON job_applications (candidate_id, idempotency_key)
    WHERE is_deleted = false;

CREATE TABLE IF NOT EXISTS job_screening_configs (
    job_id BIGINT NOT NULL PRIMARY KEY,
    enterprise_id BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    overall_threshold INT NOT NULL CHECK (overall_threshold BETWEEN 0 AND 100),
    dimension_thresholds JSONB NOT NULL DEFAULT '{}' CHECK (jsonb_typeof(dimension_thresholds) = 'object'),
    version INT NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_by BIGINT,
    updated_by BIGINT,
    trace_id VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_job_screening_configs_enterprise
    ON job_screening_configs (enterprise_id, enabled);

CREATE TABLE IF NOT EXISTS application_ai_screenings (
    id BIGINT NOT NULL PRIMARY KEY,
    application_id BIGINT NOT NULL,
    candidate_profile_id BIGINT,
    status VARCHAR(20) NOT NULL CHECK (status IN ('WAITING_PROFILE', 'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    overall_match_score INT CHECK (overall_match_score BETWEEN 0 AND 100),
    dimension_matches_json JSONB,
    recommendation VARCHAR(32) CHECK (recommendation IN ('RECOMMEND_PASS', 'RECOMMEND_REJECT')),
    threshold_snapshot JSONB NOT NULL,
    job_snapshot JSONB NOT NULL,
    llm_config_snapshot JSONB,
    review_decision VARCHAR(16) CHECK (review_decision IN ('PASSED', 'REJECTED')),
    reviewed_by BIGINT,
    reviewed_at TIMESTAMPTZ,
    attempt_count INT NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    deadline_at TIMESTAMPTZ,
    failure_reason TEXT,
    idempotency_key_hash VARCHAR(64) NOT NULL,
    trace_id VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_application_ai_screenings_application
    ON application_ai_screenings (application_id, created_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uk_application_ai_screenings_idempotency
    ON application_ai_screenings (application_id, idempotency_key_hash);
CREATE UNIQUE INDEX IF NOT EXISTS uk_application_ai_screenings_active
    ON application_ai_screenings (application_id)
    WHERE status IN ('WAITING_PROFILE', 'PENDING', 'PROCESSING');

CREATE TABLE IF NOT EXISTS candidate_job_match_analyses (
    id BIGINT NOT NULL PRIMARY KEY,
    application_id BIGINT NOT NULL,
    candidate_profile_id BIGINT,
    status VARCHAR(20) NOT NULL CHECK (status IN ('WAITING_PROFILE', 'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    match_score INT CHECK (match_score BETWEEN 0 AND 100),
    pass_probability INT CHECK (pass_probability BETWEEN 0 AND 100),
    strengths_json JSONB,
    gaps_json JSONB,
    job_snapshot JSONB NOT NULL,
    llm_config_snapshot JSONB,
    attempt_count INT NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    deadline_at TIMESTAMPTZ,
    failure_reason TEXT,
    idempotency_key_hash VARCHAR(64) NOT NULL,
    analyzed_at TIMESTAMPTZ,
    trace_id VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_candidate_job_match_application
    ON candidate_job_match_analyses (application_id, created_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uk_candidate_job_match_idempotency
    ON candidate_job_match_analyses (application_id, idempotency_key_hash);
CREATE UNIQUE INDEX IF NOT EXISTS uk_candidate_job_match_active
    ON candidate_job_match_analyses (application_id)
    WHERE status IN ('WAITING_PROFILE', 'PENDING', 'PROCESSING');


-- ==================== 15. local_message ====================
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


-- ==================== 16. llm_provider_config ====================
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


-- ==================== 17. ai_global_route ====================
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


-- ==================== 18. llm_scene_config ====================
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
    extra_options       JSONB           NOT NULL DEFAULT '{}'::jsonb,
    enabled             BOOLEAN         NOT NULL DEFAULT TRUE,
    version             INT             NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          BIGINT,
    trace_id            VARCHAR(128),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (scene_code),
    CONSTRAINT chk_scene_model_type CHECK (model_type IN ('CHAT', 'EMBEDDING')),
    CONSTRAINT chk_scene_temperature
        CHECK (temperature IS NULL OR (temperature >= 0 AND temperature <= 2)),
    CONSTRAINT chk_scene_top_p
        CHECK (top_p IS NULL OR (top_p > 0 AND top_p <= 1)),
    CONSTRAINT chk_scene_max_input_tokens
        CHECK (max_input_tokens BETWEEN 256 AND 1000000),
    CONSTRAINT chk_scene_max_output_tokens
        CHECK (max_output_tokens BETWEEN 1 AND 32768),
    CONSTRAINT chk_scene_timeout CHECK (timeout_seconds BETWEEN 5 AND 180),
    CONSTRAINT chk_scene_version CHECK (version >= 0),
    CONSTRAINT chk_scene_extra_options
        CHECK (jsonb_typeof(extra_options) = 'object')
);

CREATE INDEX IF NOT EXISTS idx_llm_scene_provider
    ON llm_scene_config (provider_id);
CREATE INDEX IF NOT EXISTS idx_llm_scene_type_enabled
    ON llm_scene_config (model_type, enabled);

COMMENT ON TABLE llm_scene_config IS 'AI 场景执行参数表';
COMMENT ON COLUMN llm_scene_config.scene_code IS '代码预定义的 AI 业务场景编码';
COMMENT ON COLUMN llm_scene_config.model_type IS 'CHAT / EMBEDDING';
COMMENT ON COLUMN llm_scene_config.provider_id IS
    '[逻辑外键]→llm_provider_config；为空时按 model_type 使用 ai_global_route';
COMMENT ON COLUMN llm_scene_config.extra_options IS
    '仅允许服务端白名单中的供应商特有参数';
COMMENT ON COLUMN llm_scene_config.version IS '管理端 CAS 版本';

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
    'RESUME_ANALYSIS', 'CHAT', NULL, 0.200, 0.900,
    16000, 2000, 60, 'v1', '{}'::jsonb,
    TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    'CANDIDATE_PROFILE_GENERATION', 'CHAT', NULL, 0.200, 0.900,
    16000, 2000, 60, 'v1', '{}'::jsonb,
    TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    'HR_APPLICATION_SCREENING', 'CHAT', NULL, 0.200, 0.900,
    16000, 1600, 60, 'v1', '{}'::jsonb,
    TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    'CANDIDATE_JOB_MATCHING', 'CHAT', NULL, 0.200, 0.900,
    16000, 1200, 60, 'v1', '{}'::jsonb,
    TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
ON CONFLICT (scene_code) DO NOTHING;


-- ============================================================
-- 种子数据：角色（幂等更新，不删除既有角色关联）
-- role_scope: PLATFORM / ENTERPRISE / USER
-- ============================================================
-- PLATFORM 域（平台运营端 Admin）
INSERT INTO sys_roles (id, role_code, role_name, role_scope, created_at) VALUES
(1001, 'SUPER_ADMIN',       '超级管理员',   'PLATFORM',   NOW()),
(1002, 'FINANCE_ADMIN',     '财务管理员',   'PLATFORM',   NOW()),
(1003, 'PLATFORM_OPS',      '平台运维',     'PLATFORM',   NOW())
ON CONFLICT (id) DO UPDATE SET
    role_code = EXCLUDED.role_code,
    role_name = EXCLUDED.role_name,
    role_scope = EXCLUDED.role_scope;

-- ENTERPRISE 域（B 端企业，通过 enterprise_team_members.role_id 分配）
INSERT INTO sys_roles (id, role_code, role_name, role_scope, created_at) VALUES
(2001, 'ENTERPRISE_OWNER',  '企业所有者',   'ENTERPRISE', NOW()),
(2002, 'ENTERPRISE_ADMIN',  '企业管理员',   'ENTERPRISE', NOW()),
(2003, 'HR_MANAGER',        'HR 经理',      'ENTERPRISE', NOW()),
(2004, 'HR_RECRUITER',      '招聘专员',     'ENTERPRISE', NOW()),
(2005, 'INTERVIEWER',       '面试官',       'ENTERPRISE', NOW())
ON CONFLICT (id) DO UPDATE SET
    role_code = EXCLUDED.role_code,
    role_name = EXCLUDED.role_name,
    role_scope = EXCLUDED.role_scope;

-- USER 域（C 端求职者，注册时 sys_user_roles 自动分配）
INSERT INTO sys_roles (id, role_code, role_name, role_scope, created_at) VALUES
(3001, 'CANDIDATE',         '求职者',       'USER',       NOW())
ON CONFLICT (id) DO UPDATE SET
    role_code = EXCLUDED.role_code,
    role_name = EXCLUDED.role_name,
    role_scope = EXCLUDED.role_scope;


-- ============================================================
-- 种子数据：sys_permissions + sys_role_permissions
-- Phase 1: 企业管理 / 团队 / 岗位 / 角色查阅 / 用户角色关联
-- Phase 2: 简历 / 投递 / 本地消息运维 / LLM 配置
-- ============================================================

-- ===== sys_permissions (Phase 1 + Phase 2 合并) =====

INSERT INTO sys_permissions (id, perm_code, perm_type, api_path, status) VALUES
-- 1.2 企业管理
(101, 'enterprise:create', 'API', '/api/v1/enterprises', 1),
(102, 'enterprise:list',   'API', '/api/v1/enterprises', 1),
(103, 'enterprise:detail', 'API', '/api/v1/enterprises/*', 1),
(104, 'enterprise:update', 'API', '/api/v1/enterprises/*', 1),
(105, 'enterprise:delete', 'API', '/api/v1/enterprises/*', 1),
(106, 'enterprise:update-contact', 'API', '/api/v1/enterprises/*/contact*', 1),

-- 1.2 团队成员管理
(111, 'team:list',   'API', '/api/v1/enterprises/*/members', 1),
(112, 'team:invite', 'API', '/api/v1/enterprises/*/members', 1),
(113, 'team:update', 'API', '/api/v1/enterprises/*/members/*', 1),
(114, 'team:remove', 'API', '/api/v1/enterprises/*/members/*', 1),

-- 1.3 角色查阅 (Admin)
(203, 'admin:roles:list',   'API', '/api/v1/admin/roles', 1),
(204, 'admin:roles:detail', 'API', '/api/v1/admin/roles/*', 1),

-- 1.3 用户角色关联 (Admin)
(221, 'admin:user-roles:assign', 'API', '/api/v1/admin/users/*/roles', 1),
(222, 'admin:user-roles:list',   'API', '/api/v1/admin/users/*/roles', 1),
(223, 'admin:user-roles:remove', 'API', '/api/v1/admin/users/*/roles', 1),

-- 1.8 平台用户管理 (Admin)
(224, 'admin:users:list',   'API', '/api/v1/admin/users', 1),
(225, 'admin:users:detail', 'API', '/api/v1/admin/users/*', 1),

-- 1.4 岗位管理
(301, 'job:create', 'API', '/api/v1/enterprises/*/jobs', 1),
(302, 'job:list',   'API', '/api/v1/enterprises/*/jobs', 1),
(303, 'job:detail', 'API', '/api/v1/enterprises/*/jobs/*', 1),
(304, 'job:update', 'API', '/api/v1/enterprises/*/jobs/*', 1),
(305, 'job:toggle-status', 'API', '/api/v1/enterprises/*/jobs/*/status', 1),
(306, 'job:delete', 'API', '/api/v1/enterprises/*/jobs/*', 1),

-- 2.1 简历管理
(401, 'resume:upload',         'API', '/api/v1/resumes', 1),
(402, 'resume:list',           'API', '/api/v1/resumes', 1),
(403, 'resume:detail',         'API', '/api/v1/resumes/*', 1),
(404, 'resume:delete',         'API', '/api/v1/resumes/*', 1),
(405, 'resume:analyze',        'API', '/api/v1/resumes/*/analyze', 1),
(406, 'resume:analysis-result','API', '/api/v1/resumes/*/analysis', 1),
(407, 'resume:download',        'API', '/api/v1/resumes/*/download', 1),

-- 2.2 投递与初筛
(411, 'application:apply',           'API', '/api/v1/jobs/*/applications', 1),
(412, 'application:list',            'API', '/api/v1/enterprises/*/jobs/*/applications', 1),
(413, 'application:detail',          'API', '/api/v1/enterprises/*/applications/*', 1),
(414, 'application:update-status',   'API', '/api/v1/enterprises/*/applications/*/status', 1),
(415, 'candidate:applications',      'API', '/api/v1/candidate/applications', 1),
(416, 'candidate:application:withdraw',
 'API', '/api/v1/candidate/applications/*/withdraw', 1),
(417, 'application:screening-config:detail', 'API', '/api/v1/enterprises/*/jobs/*/screening-config', 1),
(418, 'application:screening-config:update', 'API', '/api/v1/enterprises/*/jobs/*/screening-config', 1),
(419, 'application:ai-screening:create', 'API', '/api/v1/enterprises/*/applications/*/ai-screenings', 1),
(420, 'application:ai-screening:detail', 'API', '/api/v1/enterprises/*/applications/*/ai-screenings/latest', 1),
(426, 'application:ai-screening:review', 'API', '/api/v1/enterprises/*/applications/*/ai-screenings/*/review', 1),
(427, 'application:candidate-profile:detail', 'API', '/api/v1/enterprises/*/applications/*/candidate-profile', 1),
(428, 'candidate:application:match-analysis:create', 'API', '/api/v1/candidate/applications/*/match-analyses', 1),
(429, 'candidate:application:match-analysis:detail', 'API', '/api/v1/candidate/applications/*/match-analyses/latest', 1),

-- 2.5 本地消息管理
(421, 'ops:local-message:page',        'API', '/api/v1/admin/local-messages', 1),
(422, 'ops:local-message:detail',      'API', '/api/v1/admin/local-messages/*', 1),
(423, 'ops:local-message:retry',       'API', '/api/v1/admin/local-messages/*/retry', 1),
(424, 'ops:local-message:batch-retry', 'API', '/api/v1/admin/local-messages/batch-retry', 1),

-- 2.4 大模型 Provider、全局路由与场景配置
(431, 'admin:llm:provider:create', 'API', '/api/v1/admin/llm/providers', 1),
(432, 'admin:llm:provider:list', 'API', '/api/v1/admin/llm/providers', 1),
(433, 'admin:llm:provider:detail', 'API', '/api/v1/admin/llm/providers/*', 1),
(434, 'admin:llm:provider:update', 'API', '/api/v1/admin/llm/providers/*', 1),
(435, 'admin:llm:provider:status', 'API', '/api/v1/admin/llm/providers/*/status', 1),
(436, 'admin:llm:provider:delete', 'API', '/api/v1/admin/llm/providers/*', 1),
(437, 'admin:llm:provider:test', 'API', '/api/v1/admin/llm/providers/*/test-connection', 1),
(438, 'admin:ai:route:list', 'API', '/api/v1/admin/ai/routes', 1),
(439, 'admin:ai:route:update', 'API', '/api/v1/admin/ai/routes/*', 1),
(440, 'admin:llm:scene:list', 'API', '/api/v1/admin/llm/scenes', 1),
(441, 'admin:llm:scene:detail', 'API', '/api/v1/admin/llm/scenes/*', 1),
(442, 'admin:llm:scene:update', 'API', '/api/v1/admin/llm/scenes/*', 1),
(443, 'admin:llm:scene:status', 'API', '/api/v1/admin/llm/scenes/*/status', 1)
ON CONFLICT (id) DO UPDATE SET
    perm_code = EXCLUDED.perm_code,
    perm_type = EXCLUDED.perm_type,
    api_path = EXCLUDED.api_path,
    status = EXCLUDED.status;


-- ===== sys_role_permissions =====

-- SUPER_ADMIN (1001) — Phase 1 与 Phase 2 全部权限
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(1001, 101, NOW()), (1001, 102, NOW()), (1001, 103, NOW()), (1001, 104, NOW()), (1001, 105, NOW()), (1001, 106, NOW()),
(1001, 111, NOW()), (1001, 112, NOW()), (1001, 113, NOW()), (1001, 114, NOW()),
(1001, 203, NOW()), (1001, 204, NOW()),
(1001, 221, NOW()), (1001, 222, NOW()), (1001, 223, NOW()),
(1001, 224, NOW()), (1001, 225, NOW()),
(1001, 301, NOW()), (1001, 302, NOW()), (1001, 303, NOW()), (1001, 304, NOW()), (1001, 305, NOW()), (1001, 306, NOW()),
(1001, 401, NOW()), (1001, 402, NOW()), (1001, 403, NOW()), (1001, 404, NOW()), (1001, 405, NOW()), (1001, 406, NOW()), (1001, 407, NOW()),
(1001, 411, NOW()), (1001, 412, NOW()), (1001, 413, NOW()), (1001, 414, NOW()), (1001, 415, NOW()), (1001, 416, NOW()),
(1001, 417, NOW()), (1001, 418, NOW()), (1001, 419, NOW()), (1001, 420, NOW()),
(1001, 426, NOW()), (1001, 427, NOW()), (1001, 428, NOW()), (1001, 429, NOW()),
(1001, 421, NOW()), (1001, 422, NOW()), (1001, 423, NOW()), (1001, 424, NOW()),
(1001, 431, NOW()), (1001, 432, NOW()), (1001, 433, NOW()), (1001, 434, NOW()), (1001, 435, NOW()), (1001, 436, NOW()),
(1001, 437, NOW()), (1001, 438, NOW()), (1001, 439, NOW()), (1001, 440, NOW()), (1001, 441, NOW()), (1001, 442, NOW()), (1001, 443, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- FINANCE_ADMIN (1002) — 企业查看 + 简历查看
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(1002, 102, NOW()), (1002, 103, NOW()),
(1002, 402, NOW()), (1002, 403, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- PLATFORM_OPS (1003) — 本地消息运维与只读 LLM 配置检查
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(1003, 421, NOW()), (1003, 422, NOW()), (1003, 423, NOW()), (1003, 424, NOW()),
(1003, 432, NOW()), (1003, 433, NOW()), (1003, 437, NOW()),
(1003, 438, NOW()), (1003, 440, NOW()), (1003, 441, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ENTERPRISE_OWNER (2001) — 企业/团队/岗位/简历/投递（无 Admin 和 ops）
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2001, 101, NOW()), (2001, 102, NOW()), (2001, 103, NOW()), (2001, 104, NOW()), (2001, 105, NOW()), (2001, 106, NOW()),
(2001, 111, NOW()), (2001, 112, NOW()), (2001, 113, NOW()), (2001, 114, NOW()),
(2001, 301, NOW()), (2001, 302, NOW()), (2001, 303, NOW()), (2001, 304, NOW()), (2001, 305, NOW()), (2001, 306, NOW()),
(2001, 401, NOW()), (2001, 402, NOW()), (2001, 403, NOW()), (2001, 404, NOW()), (2001, 405, NOW()), (2001, 406, NOW()),
(2001, 412, NOW()), (2001, 413, NOW()), (2001, 414, NOW()),
(2001, 417, NOW()), (2001, 418, NOW()), (2001, 419, NOW()), (2001, 420, NOW()), (2001, 426, NOW()), (2001, 427, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ENTERPRISE_ADMIN (2002) — 与 OWNER 一致，仅去掉 enterprise:delete
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2002, 101, NOW()), (2002, 102, NOW()), (2002, 103, NOW()), (2002, 104, NOW()), (2002, 106, NOW()),
(2002, 111, NOW()), (2002, 112, NOW()), (2002, 113, NOW()), (2002, 114, NOW()),
(2002, 301, NOW()), (2002, 302, NOW()), (2002, 303, NOW()), (2002, 304, NOW()), (2002, 305, NOW()), (2002, 306, NOW()),
(2002, 401, NOW()), (2002, 402, NOW()), (2002, 403, NOW()), (2002, 404, NOW()), (2002, 405, NOW()), (2002, 406, NOW()),
(2002, 412, NOW()), (2002, 413, NOW()), (2002, 414, NOW()),
(2002, 417, NOW()), (2002, 418, NOW()), (2002, 419, NOW()), (2002, 420, NOW()), (2002, 426, NOW()), (2002, 427, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- HR_MANAGER (2003) — 团队查看/邀请/改角色 + 岗位全部 + 简历全部 + 投递管理全部
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2003, 111, NOW()), (2003, 112, NOW()), (2003, 113, NOW()),
(2003, 301, NOW()), (2003, 302, NOW()), (2003, 303, NOW()), (2003, 304, NOW()), (2003, 305, NOW()), (2003, 306, NOW()),
(2003, 401, NOW()), (2003, 402, NOW()), (2003, 403, NOW()), (2003, 404, NOW()), (2003, 405, NOW()), (2003, 406, NOW()),
(2003, 412, NOW()), (2003, 413, NOW()), (2003, 414, NOW()),
(2003, 417, NOW()), (2003, 418, NOW()), (2003, 419, NOW()), (2003, 420, NOW()), (2003, 426, NOW()), (2003, 427, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- HR_RECRUITER (2004) — 岗位发布/查看/编辑（无删除和开关）+ 简历除删除外 + 投递管理
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2004, 301, NOW()), (2004, 302, NOW()), (2004, 303, NOW()), (2004, 304, NOW()),
(2004, 401, NOW()), (2004, 402, NOW()), (2004, 403, NOW()), (2004, 405, NOW()), (2004, 406, NOW()),
(2004, 412, NOW()), (2004, 413, NOW()), (2004, 414, NOW()),
(2004, 417, NOW()), (2004, 419, NOW()), (2004, 420, NOW()), (2004, 426, NOW()), (2004, 427, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- INTERVIEWER (2005) — 仅查看岗位 + 查看简历和投递
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2005, 302, NOW()), (2005, 303, NOW()),
(2005, 402, NOW()), (2005, 403, NOW()), (2005, 406, NOW()),
(2005, 412, NOW()), (2005, 413, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- CANDIDATE (3001) — 管理本人简历、投递岗位、查看并撤回本人投递；可创建企业（创建者自动成为该企业 OWNER）
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(3001, 101, NOW()),
(3001, 401, NOW()), (3001, 402, NOW()), (3001, 403, NOW()),
(3001, 404, NOW()), (3001, 405, NOW()), (3001, 406, NOW()),
(3001, 407, NOW()),
(3001, 411, NOW()), (3001, 415, NOW()), (3001, 416, NOW()),
(3001, 428, NOW()), (3001, 429, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ============================================================
-- Phase 3 ~ Phase 9 模块脚本并入（2026-08-22，全部幂等可重复执行）
-- 原始脚本保留在 sql/3 ~ sql/9 目录，本段为合并镜像
-- ============================================================

-- >>>>>>>>>> 并入自: sql/3/phase3-sys_permissions-init.sql <<<<<<<<<<
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
    531, 532, 533, 534, 535, 536,
    537, 538, 539, 540, 541, 542, 543
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
 'API', '/api/v1/enterprises/*/interview-schedules', 1),
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
 'API', '/api/v1/candidate/interview-schedules/*/report', 1),

-- 3.5 面试会话接管
(537, 'interview-session:takeover',
 'API', '/api/v1/interview-sessions/*/takeovers', 1),
(538, 'interview-session:takeover:end',
 'API', '/api/v1/interview-sessions/*/takeovers/*/end', 1),

-- 3.6 企业 Offer 管理
(539, 'offer:create',
 'API', '/api/v1/enterprises/*/applications/*/offers', 1),
(540, 'offer:list',
 'API', '/api/v1/enterprises/*/applications/*/offers', 1),
(541, 'offer:update',
 'API', '/api/v1/enterprises/*/offers/*', 1),
(542, 'offer:send',
 'API', '/api/v1/enterprises/*/offers/*/send', 1),
(543, 'offer:withdraw',
 'API', '/api/v1/enterprises/*/offers/*/withdraw', 1)
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
(1001, 534, NOW()), (1001, 535, NOW()), (1001, 536, NOW()),
(1001, 537, NOW()), (1001, 538, NOW()), (1001, 539, NOW()),
(1001, 540, NOW()), (1001, 541, NOW()), (1001, 542, NOW()),
(1001, 543, NOW())
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

-- 企业角色：面试会话接管（HR + 面试官可接管）
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2001, 537, NOW()), (2001, 538, NOW()),
(2002, 537, NOW()), (2002, 538, NOW()),
(2003, 537, NOW()), (2003, 538, NOW()),
(2004, 537, NOW()), (2004, 538, NOW()),
(2005, 537, NOW()), (2005, 538, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 企业 HR 角色：Offer 管理（面试官与候选人不授予）
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2001, 539, NOW()), (2001, 540, NOW()), (2001, 541, NOW()),
(2001, 542, NOW()), (2001, 543, NOW()),
(2002, 539, NOW()), (2002, 540, NOW()), (2002, 541, NOW()),
(2002, 542, NOW()), (2002, 543, NOW()),
(2003, 539, NOW()), (2003, 540, NOW()), (2003, 541, NOW()),
(2003, 542, NOW()), (2003, 543, NOW()),
(2004, 539, NOW()), (2004, 540, NOW()), (2004, 541, NOW()),
(2004, 542, NOW()), (2004, 543, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- CANDIDATE (3001)：本人排期、会话和报告
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(3001, 516, NOW()), (3001, 517, NOW()), (3001, 518, NOW()),
(3001, 521, NOW()), (3001, 522, NOW()), (3001, 523, NOW()),
(3001, 525, NOW()), (3001, 526, NOW()),
(3001, 534, NOW()), (3001, 535, NOW()), (3001, 536, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

COMMIT;

-- >>>>>>>>>> 并入自: sql/3/phase3-tables-init.sql <<<<<<<<<<
-- ============================================================
-- Phase 3: 文本面试核心链路建表脚本（PostgreSQL）
-- ============================================================
-- 约束：
-- 1. 业务主键由应用侧雪花算法生成，不使用数据库自增。
-- 2. 不创建物理外键，跨表引用由所属模块在应用层校验。
-- 3. 招聘结果归 job_applications，interview_schedule 只描述单轮面试。

BEGIN;

CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ==================== 1. interview_stage_templates ====================
CREATE TABLE IF NOT EXISTS interview_stage_templates (
    id                      BIGINT          NOT NULL,
    enterprise_id           BIGINT          NOT NULL,
    template_name           VARCHAR(64)     NOT NULL,
    stages_sequence_json    JSONB           NOT NULL,
    version                 INT             NOT NULL DEFAULT 0,
    is_deleted              BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by              BIGINT,
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_interview_template_stages_array
        CHECK (jsonb_typeof(stages_sequence_json) = 'array'),
    CONSTRAINT ck_interview_template_version
        CHECK (version >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_template_enterprise_name
    ON interview_stage_templates (enterprise_id, template_name)
    WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_interview_template_enterprise_updated
    ON interview_stage_templates (enterprise_id, updated_at DESC, id DESC);

COMMENT ON TABLE interview_stage_templates IS '企业自定义面试阶段模板表';
COMMENT ON COLUMN interview_stage_templates.id IS '雪花主键';
COMMENT ON COLUMN interview_stage_templates.enterprise_id IS '[逻辑外键]→enterprises';
COMMENT ON COLUMN interview_stage_templates.stages_sequence_json IS '阶段节点编排 JSON 数组';
COMMENT ON COLUMN interview_stage_templates.version IS '管理端编辑使用的乐观锁版本号';
COMMENT ON COLUMN interview_stage_templates.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN interview_stage_templates.updated_by IS '[逻辑外键]→sys_users';

-- ==================== 2. interview_phase_configs ====================
CREATE TABLE IF NOT EXISTS interview_phase_configs (
    id                      BIGINT              NOT NULL,
    template_id             BIGINT              NOT NULL,
    phase_code              VARCHAR(32)         NOT NULL,
    question_count          INT                 NOT NULL DEFAULT 3,
    difficulty_weight       DOUBLE PRECISION    NOT NULL DEFAULT 0.5,
    prompt_override         TEXT,
    version                 INT                 NOT NULL DEFAULT 0,
    is_deleted              BOOLEAN             NOT NULL DEFAULT FALSE,
    updated_at              TIMESTAMPTZ         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_interview_phase_question_count
        CHECK (question_count BETWEEN 1 AND 20),
    CONSTRAINT ck_interview_phase_difficulty
        CHECK (difficulty_weight BETWEEN 0.0 AND 1.0),
    CONSTRAINT ck_interview_phase_code
        CHECK (phase_code ~ '^[A-Z][A-Z0-9_]{0,31}$'),
    CONSTRAINT ck_interview_phase_version
        CHECK (version >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_phase_template_code
    ON interview_phase_configs (template_id, phase_code)
    WHERE is_deleted = FALSE;

COMMENT ON TABLE interview_phase_configs IS 'AI动态组卷阶段策略配置表';
COMMENT ON COLUMN interview_phase_configs.id IS '雪花主键';
COMMENT ON COLUMN interview_phase_configs.template_id IS '[逻辑外键]→interview_stage_templates';
COMMENT ON COLUMN interview_phase_configs.phase_code IS '模板内的阶段编码';
COMMENT ON COLUMN interview_phase_configs.version IS '多管理员编辑使用的乐观锁版本号';
COMMENT ON COLUMN interview_phase_configs.is_deleted IS '逻辑删除标识';

-- ==================== 3. interview_plan_drafts ====================
CREATE TABLE IF NOT EXISTS interview_plan_drafts (
    id                      BIGINT          NOT NULL,
    enterprise_id           BIGINT          NOT NULL,
    application_id          BIGINT          NOT NULL,
    template_id             BIGINT          NOT NULL,
    requested_by            BIGINT          NOT NULL,
    idempotency_key         VARCHAR(128)    NOT NULL,
    request_json            JSONB           NOT NULL,
    input_snapshot_json     JSONB,
    plan_json               JSONB,
    status                  VARCHAR(16)     NOT NULL DEFAULT 'PENDING',
    failure_reason          VARCHAR(512),
    generation_message_id   BIGINT,
    attempt_count           INT             NOT NULL DEFAULT 0,
    generation_deadline_at  TIMESTAMPTZ,
    version                 INT             NOT NULL DEFAULT 0,
    expires_at              TIMESTAMPTZ,
    applied_at              TIMESTAMPTZ,
    apply_idempotency_key   VARCHAR(128),
    applied_plan_json       JSONB,
    applied_schedule_ids    JSONB,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by              BIGINT,
    trace_id                VARCHAR(128),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_interview_plan_request_object
        CHECK (jsonb_typeof(request_json) = 'object'),
    CONSTRAINT ck_interview_plan_input_object
        CHECK (input_snapshot_json IS NULL OR jsonb_typeof(input_snapshot_json) = 'object'),
    CONSTRAINT ck_interview_plan_result_object
        CHECK (plan_json IS NULL OR jsonb_typeof(plan_json) = 'object'),
    CONSTRAINT ck_interview_plan_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'READY', 'APPLIED', 'FAILED', 'EXPIRED')),
    CONSTRAINT ck_interview_plan_version
        CHECK (version >= 0),
    CONSTRAINT ck_interview_plan_applied_object
        CHECK (applied_plan_json IS NULL OR jsonb_typeof(applied_plan_json) = 'object'),
    CONSTRAINT ck_interview_plan_applied_ids
        CHECK (applied_schedule_ids IS NULL OR jsonb_typeof(applied_schedule_ids) = 'array'),
    CONSTRAINT ck_interview_plan_generation_attempt
        CHECK (attempt_count >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_plan_idempotency
    ON interview_plan_drafts (enterprise_id, application_id, requested_by, idempotency_key);
CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_plan_application_active
    ON interview_plan_drafts (application_id)
    WHERE status IN ('PENDING', 'PROCESSING', 'READY');
CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_plan_generation_message
    ON interview_plan_drafts (generation_message_id)
    WHERE generation_message_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_interview_plan_application_created
    ON interview_plan_drafts (enterprise_id, application_id, created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_interview_plan_generation_timeout
    ON interview_plan_drafts (generation_deadline_at, id)
    WHERE status = 'PROCESSING';

COMMENT ON TABLE interview_plan_drafts IS 'Agent 面试编排业务草案表';
COMMENT ON COLUMN interview_plan_drafts.application_id IS '[逻辑外键]→job_applications';
COMMENT ON COLUMN interview_plan_drafts.template_id IS '[逻辑外键]→interview_stage_templates';
COMMENT ON COLUMN interview_plan_drafts.request_json IS '创建草案时的编排参数快照';
COMMENT ON COLUMN interview_plan_drafts.input_snapshot_json IS 'Agent 使用的非敏感输入快照';
COMMENT ON COLUMN interview_plan_drafts.plan_json IS '阶段、题纲和排期建议结构化结果';
COMMENT ON COLUMN interview_plan_drafts.generation_message_id IS '[逻辑引用]→local_message，不建跨模块外键';
COMMENT ON COLUMN interview_plan_drafts.version IS 'HR 确认草案使用的乐观锁版本';
COMMENT ON COLUMN interview_plan_drafts.apply_idempotency_key IS 'HR 应用草案时的幂等键，仅 APPLIED 后有值';
COMMENT ON COLUMN interview_plan_drafts.applied_plan_json IS 'HR 应用时提交的修订后计划快照，用于幂等对比与页面刷新恢复';
COMMENT ON COLUMN interview_plan_drafts.applied_schedule_ids IS '应用时创建的排期 ID 数组快照，用于幂等重放';

-- ==================== 4. candidate_interview_availability ====================
CREATE TABLE IF NOT EXISTS candidate_interview_availability (
    candidate_user_id BIGINT       NOT NULL PRIMARY KEY,
    timezone          VARCHAR(64)  NOT NULL,
    ranges_json       JSONB        NOT NULL DEFAULT '[]'::JSONB,
    version           INT          NOT NULL DEFAULT 0,
    updated_by        BIGINT,
    trace_id          VARCHAR(128),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_candidate_availability_ranges
        CHECK (jsonb_typeof(ranges_json) = 'array'),
    CONSTRAINT ck_candidate_availability_version
        CHECK (version >= 0)
);

COMMENT ON TABLE candidate_interview_availability IS '候选人可面试时间聚合配置表';
COMMENT ON COLUMN candidate_interview_availability.candidate_user_id IS '[逻辑外键]→sys_users，同时作为主键';
COMMENT ON COLUMN candidate_interview_availability.timezone IS 'IANA 时区';
COMMENT ON COLUMN candidate_interview_availability.ranges_json IS '按开始时间排序的可面试时间范围数组';
COMMENT ON COLUMN candidate_interview_availability.version IS '完整替换配置使用的乐观锁版本';

-- ==================== 5. interview_schedule ====================
CREATE TABLE IF NOT EXISTS interview_schedule (
    id                      BIGINT          NOT NULL,
    enterprise_id           BIGINT          NOT NULL,
    company_user_id         BIGINT          NOT NULL,
    interviewer_user_id     BIGINT,
    application_id          BIGINT          NOT NULL,
    template_id             BIGINT          NOT NULL,
    round_no                SMALLINT        NOT NULL DEFAULT 1,
    phase_code              VARCHAR(32)     NOT NULL,
    template_snapshot_json  JSONB           NOT NULL,
    idempotency_key         VARCHAR(128)    NOT NULL,
    interview_time          TIMESTAMPTZ     NOT NULL,
    duration_minutes        INT             NOT NULL DEFAULT 60,
    interview_type          VARCHAR(16)     NOT NULL DEFAULT 'TEXT',
    status                  VARCHAR(32)     NOT NULL DEFAULT 'PENDING_CONFIRMATION',
    status_reason           VARCHAR(256),
    version                 INT             NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted              BOOLEAN         NOT NULL DEFAULT FALSE,
    updated_by              BIGINT,
    trace_id                VARCHAR(128),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_interview_schedule_round
        CHECK (round_no > 0),
    CONSTRAINT ck_interview_schedule_phase_code
        CHECK (phase_code ~ '^[A-Z][A-Z0-9_]{0,31}$'),
    CONSTRAINT ck_interview_schedule_template_snapshot
        CHECK (jsonb_typeof(template_snapshot_json) = 'object'),
    CONSTRAINT ck_interview_schedule_duration
        CHECK (duration_minutes BETWEEN 15 AND 480),
    CONSTRAINT ck_interview_schedule_type
        CHECK (interview_type IN ('TEXT', 'VOICE', 'CODE')),
    CONSTRAINT ck_interview_schedule_status
        CHECK (status IN (
            'PENDING_CONFIRMATION', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED',
            'DECLINED', 'CANCELLED', 'NO_SHOW'
        )),
    CONSTRAINT ck_interview_schedule_version
        CHECK (version >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_schedule_application_round_active
    ON interview_schedule (application_id, round_no)
    WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_schedule_application_phase_active
    ON interview_schedule (application_id, phase_code)
    WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_schedule_idempotency
    ON interview_schedule (enterprise_id, idempotency_key);
CREATE INDEX IF NOT EXISTS idx_interview_schedule_enterprise_time
    ON interview_schedule (enterprise_id, interview_time, id)
    WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_interview_schedule_interviewer_time
    ON interview_schedule (interviewer_user_id, interview_time, id)
    WHERE is_deleted = FALSE
      AND interviewer_user_id IS NOT NULL
      AND status IN ('PENDING_CONFIRMATION', 'CONFIRMED', 'IN_PROGRESS');
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'interview_schedule'::regclass
          AND conname = 'ex_interview_schedule_interviewer_time'
    ) THEN
        ALTER TABLE interview_schedule
            ADD CONSTRAINT ex_interview_schedule_interviewer_time
            EXCLUDE USING gist (
                interviewer_user_id WITH =,
                tsrange(
                    interview_time AT TIME ZONE 'UTC',
                    (interview_time AT TIME ZONE 'UTC')
                        + duration_minutes * INTERVAL '1 minute',
                    '[)'
                ) WITH &&
            )
            WHERE (
                is_deleted = FALSE
                AND interviewer_user_id IS NOT NULL
                AND status IN ('PENDING_CONFIRMATION', 'CONFIRMED', 'IN_PROGRESS')
            );
    END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_interview_schedule_application
    ON interview_schedule (application_id, round_no DESC);

COMMENT ON TABLE interview_schedule IS '单轮面试排期及面试生命周期表';
COMMENT ON COLUMN interview_schedule.id IS '雪花主键';
COMMENT ON COLUMN interview_schedule.enterprise_id IS '[逻辑外键]→enterprises';
COMMENT ON COLUMN interview_schedule.company_user_id IS '[逻辑外键]→sys_users，创建排期的企业用户';
COMMENT ON COLUMN interview_schedule.interviewer_user_id IS '[逻辑外键]→sys_users，被分配的面试官';
COMMENT ON COLUMN interview_schedule.application_id IS '[逻辑外键]→job_applications，候选人和岗位由投递记录确定';
COMMENT ON COLUMN interview_schedule.template_id IS '[逻辑外键]→interview_stage_templates';
COMMENT ON COLUMN interview_schedule.round_no IS '投递下的面试轮次，对应模板阶段 sortOrder';
COMMENT ON COLUMN interview_schedule.phase_code IS '由模板快照按 round_no 派生的阶段编码';
COMMENT ON COLUMN interview_schedule.template_snapshot_json IS '首轮排期固化的完整模板及阶段组卷配置快照';
COMMENT ON COLUMN interview_schedule.idempotency_key IS '创建排期请求幂等键';
COMMENT ON COLUMN interview_schedule.status IS 'PENDING_CONFIRMATION / CONFIRMED / IN_PROGRESS / COMPLETED / DECLINED / CANCELLED / NO_SHOW';
COMMENT ON COLUMN interview_schedule.version IS '排期管理操作使用的乐观锁版本号';
COMMENT ON COLUMN interview_schedule.updated_by IS '[逻辑外键]→sys_users';

-- ==================== 6. interview_sessions ====================
CREATE TABLE IF NOT EXISTS interview_sessions (
    id                      BIGINT          NOT NULL,
    enterprise_id           BIGINT          NOT NULL,
    user_id                 BIGINT          NOT NULL,
    schedule_id             BIGINT          NOT NULL,
    attempt_no              SMALLINT        NOT NULL DEFAULT 1,
    session_type            VARCHAR(16)     NOT NULL,
    idempotency_key         VARCHAR(128)    NOT NULL,
    total_questions         INT,
    current_question_index  INT             NOT NULL DEFAULT 0,
    last_event_sequence     BIGINT          NOT NULL DEFAULT 0,
    status                  VARCHAR(32)     NOT NULL DEFAULT 'CREATED',
    started_at              TIMESTAMPTZ,
    ended_at                TIMESTAMPTZ,
    termination_reason      VARCHAR(256),
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted              BOOLEAN         NOT NULL DEFAULT FALSE,
    trace_id                VARCHAR(128),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_interview_session_attempt
        CHECK (attempt_no > 0),
    CONSTRAINT ck_interview_session_type
        CHECK (session_type IN ('TEXT', 'VOICE', 'CODE')),
    CONSTRAINT ck_interview_session_question_count
        CHECK (total_questions IS NULL OR total_questions > 0),
    CONSTRAINT ck_interview_session_question_index
        CHECK (current_question_index >= 0),
    CONSTRAINT ck_interview_session_event_sequence
        CHECK (last_event_sequence >= 0),
    CONSTRAINT ck_interview_session_status
        CHECK (status IN ('CREATED', 'IN_PROGRESS', 'COMPLETED', 'TERMINATED')),
    CONSTRAINT ck_interview_session_time_order
        CHECK (ended_at IS NULL OR started_at IS NULL OR ended_at >= started_at)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_session_schedule_attempt_active
    ON interview_sessions (schedule_id, attempt_no)
    WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_session_idempotency
    ON interview_sessions (schedule_id, idempotency_key);
CREATE INDEX IF NOT EXISTS idx_interview_session_user_created
    ON interview_sessions (user_id, created_at DESC, id DESC)
    WHERE is_deleted = FALSE;

COMMENT ON TABLE interview_sessions IS '多模态面试统一运行时会话表';
COMMENT ON COLUMN interview_sessions.id IS '雪花主键';
COMMENT ON COLUMN interview_sessions.enterprise_id IS '[逻辑外键]→enterprises';
COMMENT ON COLUMN interview_sessions.user_id IS '[逻辑外键]→sys_users，候选人用户';
COMMENT ON COLUMN interview_sessions.schedule_id IS '[逻辑外键]→interview_schedule';
COMMENT ON COLUMN interview_sessions.idempotency_key IS '创建会话请求幂等键';
COMMENT ON COLUMN interview_sessions.last_event_sequence IS '已持久化语义事件的最后序号，使用原子条件更新推进';

-- ==================== 7. interview_answers ====================
CREATE TABLE IF NOT EXISTS interview_answers (
    id                      BIGINT          NOT NULL,
    session_id              BIGINT          NOT NULL,
    enterprise_id           BIGINT          NOT NULL,
    question_index          INT             NOT NULL,
    question_text           TEXT            NOT NULL,
    parent_message_id       BIGINT,
    follow_up_depth         SMALLINT        NOT NULL DEFAULT 0,
    idempotency_key         VARCHAR(128),
    user_answer             TEXT,
    score                   INT,
    ai_feedback             TEXT,
    answered_at             TIMESTAMPTZ,
    trace_id                VARCHAR(128),
    is_deleted              BOOLEAN         NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT ck_interview_answer_question_index
        CHECK (question_index >= 0),
    CONSTRAINT ck_interview_answer_follow_up_depth
        CHECK (follow_up_depth BETWEEN 0 AND 3),
    CONSTRAINT ck_interview_answer_score
        CHECK (score IS NULL OR score BETWEEN 0 AND 100),
    CONSTRAINT ck_interview_answer_answered
        CHECK (answered_at IS NULL OR user_answer IS NOT NULL)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_answer_idempotency
    ON interview_answers (session_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_interview_answer_session_order
    ON interview_answers (session_id, question_index, id)
    WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_interview_answer_parent
    ON interview_answers (parent_message_id)
    WHERE parent_message_id IS NOT NULL AND is_deleted = FALSE;

COMMENT ON TABLE interview_answers IS '文本面试问题、回答及单题AI评估记录表';
COMMENT ON COLUMN interview_answers.id IS '雪花主键，同时作为对外 questionId';
COMMENT ON COLUMN interview_answers.session_id IS '[逻辑外键]→interview_sessions';
COMMENT ON COLUMN interview_answers.enterprise_id IS '[逻辑外键]→enterprises';
COMMENT ON COLUMN interview_answers.parent_message_id IS '[逻辑外键]→interview_answers，追问所属的上一条消息';
COMMENT ON COLUMN interview_answers.idempotency_key IS '提交答案请求幂等键，题目未作答时为空';
COMMENT ON COLUMN interview_answers.answered_at IS '实际提交答案时间，题目生成后尚未回答时为空';

-- ==================== 8. interview_timeline_events ====================
CREATE TABLE IF NOT EXISTS interview_timeline_events (
    id                      BIGINT          NOT NULL,
    session_id              BIGINT          NOT NULL,
    enterprise_id           BIGINT          NOT NULL,
    event_id                VARCHAR(64)     NOT NULL,
    sequence_num            BIGINT          NOT NULL,
    event_type              VARCHAR(32)     NOT NULL,
    actor_type              VARCHAR(16)     NOT NULL,
    actor_user_id           BIGINT,
    payload_json            JSONB           NOT NULL,
    occurred_at             TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trace_id                VARCHAR(128),
    PRIMARY KEY (id),
    CONSTRAINT ck_interview_timeline_sequence
        CHECK (sequence_num > 0),
    CONSTRAINT ck_interview_timeline_actor
        CHECK (actor_type IN ('AI', 'CANDIDATE', 'INTERVIEWER', 'SYSTEM')),
    CONSTRAINT ck_interview_timeline_payload
        CHECK (jsonb_typeof(payload_json) = 'object')
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_timeline_session_sequence
    ON interview_timeline_events (session_id, sequence_num);
CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_timeline_session_event
    ON interview_timeline_events (session_id, event_id);
CREATE INDEX IF NOT EXISTS idx_interview_timeline_session_time
    ON interview_timeline_events (session_id, occurred_at, id);

COMMENT ON TABLE interview_timeline_events IS '面试断线补发与完整回放使用的语义事件时间线';
COMMENT ON COLUMN interview_timeline_events.id IS '雪花主键';
COMMENT ON COLUMN interview_timeline_events.session_id IS '[逻辑外键]→interview_sessions';
COMMENT ON COLUMN interview_timeline_events.enterprise_id IS '[逻辑外键]→enterprises';
COMMENT ON COLUMN interview_timeline_events.event_id IS '会话内事件幂等ID';
COMMENT ON COLUMN interview_timeline_events.sequence_num IS '会话内严格递增序号';
COMMENT ON COLUMN interview_timeline_events.payload_json IS '完整语义事件载荷，不保存高频增量片段或心跳';

-- ==================== 9. interview_takeovers ====================
CREATE TABLE IF NOT EXISTS interview_takeovers (
    id                      BIGINT          NOT NULL,
    session_id              BIGINT          NOT NULL,
    enterprise_id           BIGINT          NOT NULL,
    interviewer_user_id     BIGINT          NOT NULL,
    request_id              VARCHAR(64)     NOT NULL,
    status                  VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    reason                  VARCHAR(256),
    started_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at                TIMESTAMPTZ,
    trace_id                VARCHAR(128),
    PRIMARY KEY (id),
    CONSTRAINT ck_interview_takeover_status
        CHECK (status IN ('ACTIVE', 'ENDED', 'REVOKED')),
    CONSTRAINT ck_interview_takeover_time_order
        CHECK (ended_at IS NULL OR ended_at >= started_at)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_takeover_request
    ON interview_takeovers (session_id, request_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_takeover_active
    ON interview_takeovers (session_id)
    WHERE status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_interview_takeover_session_time
    ON interview_takeovers (session_id, started_at DESC, id DESC);

COMMENT ON TABLE interview_takeovers IS '面试官人工接管会话记录表';
COMMENT ON COLUMN interview_takeovers.id IS '雪花主键';
COMMENT ON COLUMN interview_takeovers.session_id IS '[逻辑外键]→interview_sessions';
COMMENT ON COLUMN interview_takeovers.enterprise_id IS '[逻辑外键]→enterprises';
COMMENT ON COLUMN interview_takeovers.interviewer_user_id IS '[逻辑外键]→sys_users';
COMMENT ON COLUMN interview_takeovers.request_id IS '接管命令幂等ID';

-- ==================== 10. interview_reports ====================
CREATE TABLE IF NOT EXISTS interview_reports (
    id                      BIGINT          NOT NULL,
    enterprise_id           BIGINT          NOT NULL,
    schedule_id             BIGINT          NOT NULL,
    generation_status       VARCHAR(16)     NOT NULL DEFAULT 'PENDING',
    failure_reason          VARCHAR(512),
    overall_ai_score        INT,
    executive_summary       TEXT,
    code_capability_review  TEXT,
    communication_score     INT,
    report_pdf_url          VARCHAR(512),
    completed_at            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trace_id                VARCHAR(128),
    is_deleted              BOOLEAN         NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT ck_interview_report_status
        CHECK (generation_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_interview_report_overall_score
        CHECK (overall_ai_score IS NULL OR overall_ai_score BETWEEN 0 AND 100),
    CONSTRAINT ck_interview_report_communication_score
        CHECK (communication_score IS NULL OR communication_score BETWEEN 0 AND 100),
    CONSTRAINT ck_interview_report_completion
        CHECK (
            (generation_status = 'COMPLETED' AND completed_at IS NOT NULL)
            OR generation_status <> 'COMPLETED'
        )
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_interview_report_schedule_active
    ON interview_reports (schedule_id)
    WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_interview_report_enterprise_status
    ON interview_reports (enterprise_id, generation_status, created_at DESC, id DESC)
    WHERE is_deleted = FALSE;

COMMENT ON TABLE interview_reports IS '面试报告生成状态及最终报告内容表';
COMMENT ON COLUMN interview_reports.id IS '雪花主键';
COMMENT ON COLUMN interview_reports.enterprise_id IS '[逻辑外键]→enterprises';
COMMENT ON COLUMN interview_reports.schedule_id IS '[逻辑外键]→interview_schedule';
COMMENT ON COLUMN interview_reports.generation_status IS 'PENDING / PROCESSING / COMPLETED / FAILED';
COMMENT ON COLUMN interview_reports.failure_reason IS '报告生成失败的受控原因';
COMMENT ON COLUMN interview_reports.report_pdf_url IS '报告PDF对象存储定位信息，不直接作为永久公网地址使用';

-- ==================== 11. application_transition_logs ====================
CREATE TABLE IF NOT EXISTS application_transition_logs (
    id                      BIGINT          NOT NULL,
    enterprise_id           BIGINT          NOT NULL,
    application_id          BIGINT          NOT NULL,
    from_status             VARCHAR(32)     NOT NULL,
    to_status               VARCHAR(32)     NOT NULL,
    operator_user_id        BIGINT          NOT NULL,
    transition_reason       VARCHAR(256),
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trace_id                VARCHAR(128),
    PRIMARY KEY (id),
    CONSTRAINT ck_application_transition_changed
        CHECK (from_status <> to_status)
);

CREATE INDEX IF NOT EXISTS idx_application_transition_application_time
    ON application_transition_logs (application_id, created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_application_transition_enterprise_time
    ON application_transition_logs (enterprise_id, created_at DESC, id DESC);

COMMENT ON TABLE application_transition_logs IS '投递聚合的招聘状态流转历史日志';
COMMENT ON COLUMN application_transition_logs.id IS '雪花主键';
COMMENT ON COLUMN application_transition_logs.enterprise_id IS '[逻辑外键]→enterprises，冗余租户隔离键';
COMMENT ON COLUMN application_transition_logs.application_id IS '[逻辑外键]→job_applications';
COMMENT ON COLUMN application_transition_logs.operator_user_id IS '[逻辑外键]→sys_users，系统自动推进时为0';

-- ==================== 12. offers ====================
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

-- >>>>>>>>>> 并入自: sql/4/phase4-sys_permissions-init.sql <<<<<<<<<<
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

-- >>>>>>>>>> 并入自: sql/4/phase4-tables-init.sql <<<<<<<<<<
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

-- >>>>>>>>>> 并入自: sql/5/phase5-sys_permissions-init.sql <<<<<<<<<<
-- 第五阶段：语音面试权限初始化
-- 统一会话的 join-token、detail、end 权限由第三阶段管理；
-- 本阶段仅新增语音消息和语音评估两个资源权限。
-- sys_role_permissions 主键为 (role_id, permission_id)，无 id 字段。

BEGIN;

-- 清理旧版独立 /voice-sessions 状态机权限及角色绑定。
DELETE FROM sys_role_permissions
WHERE permission_id IN (
    SELECT id
    FROM sys_permissions
    WHERE id IN (701, 702, 703)
       OR perm_code IN (
           'voice:session:start',
           'voice:session:status',
           'voice:session:end'
       )
);

DELETE FROM sys_permissions
WHERE id IN (701, 702, 703)
   OR perm_code IN (
       'voice:session:start',
       'voice:session:status',
       'voice:session:end'
   );

-- 第五阶段语音专属只读资源。
INSERT INTO sys_permissions (id, perm_code, perm_type, api_path, status) VALUES
(704, 'voice:messages:list',
 'API', '/api/v1/interview-sessions/*/voice/messages', 1),
(705, 'voice:evaluation:get',
 'API', '/api/v1/interview-sessions/*/voice/evaluation', 1)
ON CONFLICT (id) DO UPDATE SET
    perm_code = EXCLUDED.perm_code,
    perm_type = EXCLUDED.perm_type,
    api_path = EXCLUDED.api_path,
    status = EXCLUDED.status;

-- 平台超级管理员：排障和审计。
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(1001, 704, NOW()),
(1001, 705, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 企业所有者、管理员、HR 和面试官：仍需在接口层校验企业归属。
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2001, 704, NOW()), (2001, 705, NOW()),
(2002, 704, NOW()), (2002, 705, NOW()),
(2003, 704, NOW()), (2003, 705, NOW()),
(2004, 704, NOW()), (2004, 705, NOW()),
(2005, 704, NOW()), (2005, 705, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 候选人：仍需在接口层校验 session.user_id 为当前用户。
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(3001, 704, NOW()),
(3001, 705, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

COMMIT;

-- >>>>>>>>>> 并入自: sql/5/phase5-tables-init.sql <<<<<<<<<<
-- 第五阶段：语音面试表结构初始化
-- PostgreSQL；跨模块关联仅使用逻辑外键，不创建物理 FOREIGN KEY。
-- 会话生命周期以 interview_sessions 为唯一事实来源。

BEGIN;

-- 语音面试扩展表
CREATE TABLE IF NOT EXISTS voice_interview_sessions
(
    id                      BIGINT       PRIMARY KEY,
    interview_session_id    BIGINT       NOT NULL,
    skill_code              VARCHAR(64),
    current_phase           VARCHAR(32)  NOT NULL DEFAULT 'INTRO',
    actual_duration_seconds INT          NOT NULL DEFAULT 0,
    recording_object_key    VARCHAR(512),
    recording_consent_at    TIMESTAMPTZ,
    retention_until         TIMESTAMPTZ,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trace_id                VARCHAR(128),
    CONSTRAINT uq_voice_interview_sessions_session
        UNIQUE (interview_session_id),
    CONSTRAINT chk_voice_interview_sessions_phase
        CHECK (current_phase IN ('INTRO', 'TECH', 'PROJECT', 'HR')),
    CONSTRAINT chk_voice_interview_sessions_duration
        CHECK (actual_duration_seconds >= 0),
    CONSTRAINT chk_voice_interview_sessions_recording
        CHECK (
            recording_object_key IS NULL
            OR (recording_consent_at IS NOT NULL AND retention_until IS NOT NULL)
        )
);

COMMENT ON TABLE voice_interview_sessions IS '语音面试扩展表；会话身份和生命周期由 interview_sessions 管理';
COMMENT ON COLUMN voice_interview_sessions.id IS '雪花主键';
COMMENT ON COLUMN voice_interview_sessions.interview_session_id IS '逻辑关联 interview_sessions.id';
COMMENT ON COLUMN voice_interview_sessions.recording_object_key IS '获得明确授权后保存的私有录音对象键';
COMMENT ON COLUMN voice_interview_sessions.retention_until IS '录音及关联转写的保留截止时间';

-- 语音消息明细表
CREATE TABLE IF NOT EXISTS voice_interview_messages
(
    id                   BIGINT       PRIMARY KEY,
    interview_session_id BIGINT       NOT NULL,
    enterprise_id        BIGINT       NOT NULL,
    event_id             VARCHAR(64)  NOT NULL,
    message_type         VARCHAR(20)  NOT NULL,
    current_phase        VARCHAR(32),
    parent_message_id    BIGINT,
    follow_up_depth      SMALLINT     NOT NULL DEFAULT 0,
    asr_text             TEXT,
    llm_response_text    TEXT,
    sequence_num         BIGINT       NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trace_id             VARCHAR(128) NOT NULL,
    is_deleted           BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_voice_interview_messages_type
        CHECK (message_type IN ('USER_SPEECH', 'AI_SPEECH', 'SYSTEM')),
    CONSTRAINT chk_voice_interview_messages_depth
        CHECK (follow_up_depth >= 0),
    CONSTRAINT chk_voice_interview_messages_content
        CHECK (
            (message_type = 'USER_SPEECH' AND NULLIF(BTRIM(asr_text), '') IS NOT NULL)
            OR (message_type = 'AI_SPEECH' AND NULLIF(BTRIM(llm_response_text), '') IS NOT NULL)
            OR message_type = 'SYSTEM'
        )
);

COMMENT ON TABLE voice_interview_messages IS '语音面试最终 ASR 与完整 AI 回复明细；事件顺序以 interview_timeline_events 为准';
COMMENT ON COLUMN voice_interview_messages.id IS '雪花主键';
COMMENT ON COLUMN voice_interview_messages.interview_session_id IS '逻辑关联 interview_sessions.id';
COMMENT ON COLUMN voice_interview_messages.enterprise_id IS '逻辑关联 enterprises.id，租户隔离键';
COMMENT ON COLUMN voice_interview_messages.event_id IS '与 interview_timeline_events 共享的事件幂等 ID';
COMMENT ON COLUMN voice_interview_messages.parent_message_id IS '逻辑关联 voice_interview_messages.id';
COMMENT ON COLUMN voice_interview_messages.sequence_num IS '从 interview_sessions.last_event_sequence 原子分配';

CREATE UNIQUE INDEX IF NOT EXISTS uq_voice_messages_session_sequence
    ON voice_interview_messages (interview_session_id, sequence_num);

CREATE UNIQUE INDEX IF NOT EXISTS uq_voice_messages_session_event
    ON voice_interview_messages (interview_session_id, event_id);

CREATE INDEX IF NOT EXISTS idx_voice_messages_enterprise_created
    ON voice_interview_messages (enterprise_id, created_at DESC, id DESC);

-- 语音面试异步评估任务与结果表
CREATE TABLE IF NOT EXISTS voice_interview_evaluations
(
    id                          BIGINT       PRIMARY KEY,
    interview_session_id        BIGINT       NOT NULL,
    enterprise_id               BIGINT       NOT NULL,
    evaluation_status           VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    attempt_no                  INT          NOT NULL DEFAULT 0,
    failure_reason              VARCHAR(512),
    overall_score               INT,
    question_evaluations_json   JSONB,
    strengths_json              JSONB,
    improvements_json           JSONB,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at                  TIMESTAMPTZ,
    completed_at                TIMESTAMPTZ,
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted                  BOOLEAN      NOT NULL DEFAULT FALSE,
    trace_id                    VARCHAR(128) NOT NULL,
    CONSTRAINT uq_voice_evaluations_session
        UNIQUE (interview_session_id),
    CONSTRAINT chk_voice_evaluations_status
        CHECK (evaluation_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_voice_evaluations_attempt
        CHECK (attempt_no >= 0),
    CONSTRAINT chk_voice_evaluations_score
        CHECK (overall_score IS NULL OR overall_score BETWEEN 0 AND 100),
    CONSTRAINT chk_voice_evaluations_completed
        CHECK (
            evaluation_status <> 'COMPLETED'
            OR (overall_score IS NOT NULL AND completed_at IS NOT NULL)
        ),
    CONSTRAINT chk_voice_evaluations_failed
        CHECK (
            evaluation_status <> 'FAILED'
            OR (NULLIF(BTRIM(failure_reason), '') IS NOT NULL AND completed_at IS NOT NULL)
        ),
    CONSTRAINT chk_voice_evaluations_questions_json
        CHECK (
            question_evaluations_json IS NULL
            OR JSONB_TYPEOF(question_evaluations_json) = 'array'
        ),
    CONSTRAINT chk_voice_evaluations_strengths_json
        CHECK (
            strengths_json IS NULL
            OR JSONB_TYPEOF(strengths_json) = 'array'
        ),
    CONSTRAINT chk_voice_evaluations_improvements_json
        CHECK (
            improvements_json IS NULL
            OR JSONB_TYPEOF(improvements_json) = 'array'
        )
);

COMMENT ON TABLE voice_interview_evaluations IS '语音面试异步评估任务状态与单场结果快照';
COMMENT ON COLUMN voice_interview_evaluations.id IS '雪花主键';
COMMENT ON COLUMN voice_interview_evaluations.interview_session_id IS '逻辑关联 interview_sessions.id；一场会话仅一份评估';
COMMENT ON COLUMN voice_interview_evaluations.enterprise_id IS '逻辑关联 enterprises.id，租户隔离键';
COMMENT ON COLUMN voice_interview_evaluations.attempt_no IS '已经开始的评估尝试次数';

CREATE INDEX IF NOT EXISTS idx_voice_evaluations_pending
    ON voice_interview_evaluations (evaluation_status, updated_at)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_voice_evaluations_enterprise_created
    ON voice_interview_evaluations (enterprise_id, created_at DESC, id DESC)
    WHERE is_deleted = FALSE;

COMMIT;

-- >>>>>>>>>> 并入自: sql/6/phase6-sys_permissions-init.sql <<<<<<<<<<
-- 第六阶段：在线代码沙箱权限初始化
-- 来源：docs/第6阶段/phase6-sys_permissions-init.sql，扩展 code-test-case:update (813)。
-- sys_role_permissions 主键为 (role_id, permission_id)，无 id 字段。
-- 全部使用 ON CONFLICT DO NOTHING，脚本可重复执行。

BEGIN;

-- ===== sys_permissions =====

INSERT INTO sys_permissions (id, perm_code, perm_type, api_path, status) VALUES
-- 6.1 编程题库管理 (Admin / Enterprise 复用同一权限码，作用域由接口 scope 决定)
(801, 'code-question:create', 'API', '/api/v1/admin/code-questions', 1),
(802, 'code-question:list',   'API', '/api/v1/admin/code-questions', 1),
(803, 'code-question:detail', 'API', '/api/v1/admin/code-questions/*', 1),
(804, 'code-question:update', 'API', '/api/v1/admin/code-questions/*', 1),
(805, 'code-question:delete', 'API', '/api/v1/admin/code-questions/*', 1),

-- 6.1 测试用例管理 (Admin / Enterprise)
(811, 'code-test-case:create', 'API', '/api/v1/admin/code-questions/*/test-cases', 1),
(812, 'code-test-case:delete', 'API', '/api/v1/admin/code-questions/*/test-cases/*', 1),
(813, 'code-test-case:update', 'API', '/api/v1/admin/code-questions/*/test-cases/*', 1),

-- 6.2 代码提交与评测
(821, 'code-submission:submit',  'API', '/api/v1/interview-sessions/*/code-submit', 1),
(822, 'code-submission:results', 'API', '/api/v1/interview-sessions/*/code-results', 1)
ON CONFLICT (id) DO NOTHING;

-- ===== sys_role_permissions =====

-- SUPER_ADMIN (1001) — 全部 10 个权限
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(1001, 801, NOW()), (1001, 802, NOW()), (1001, 803, NOW()), (1001, 804, NOW()), (1001, 805, NOW()),
(1001, 811, NOW()), (1001, 812, NOW()), (1001, 813, NOW()),
(1001, 821, NOW()), (1001, 822, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- FINANCE_ADMIN (1002) — 无代码沙箱权限

-- ENTERPRISE_OWNER (2001) — 编程题库全部 + 提交与结果
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2001, 801, NOW()), (2001, 802, NOW()), (2001, 803, NOW()), (2001, 804, NOW()), (2001, 805, NOW()),
(2001, 811, NOW()), (2001, 812, NOW()), (2001, 813, NOW()),
(2001, 821, NOW()), (2001, 822, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ENTERPRISE_ADMIN (2002) — 与 OWNER 一致
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2002, 801, NOW()), (2002, 802, NOW()), (2002, 803, NOW()), (2002, 804, NOW()), (2002, 805, NOW()),
(2002, 811, NOW()), (2002, 812, NOW()), (2002, 813, NOW()),
(2002, 821, NOW()), (2002, 822, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- HR_MANAGER (2003) — 查看题库 + 提交与结果
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2003, 802, NOW()), (2003, 803, NOW()),
(2003, 821, NOW()), (2003, 822, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- HR_RECRUITER (2004) — 查看题库 + 提交与结果
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2004, 802, NOW()), (2004, 803, NOW()),
(2004, 821, NOW()), (2004, 822, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- INTERVIEWER (2005) — 查看题库 + 查询结果（不可提交）
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2005, 802, NOW()), (2005, 803, NOW()),
(2005, 822, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- CANDIDATE (3001) — 提交代码 + 查询结果
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(3001, 821, NOW()), (3001, 822, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

COMMIT;

-- >>>>>>>>>> 并入自: sql/6/phase6-tables-init.sql <<<<<<<<<<
-- 第六阶段：在线代码沙箱表结构初始化
-- PostgreSQL；跨模块关联仅使用逻辑外键，不创建物理 FOREIGN KEY。
-- 会话身份归 interview_sessions（Phase 3）所有，本阶段仅保存题目、用例与提交结果。

BEGIN;

-- 编程题库表
CREATE TABLE IF NOT EXISTS code_questions
(
    id                     BIGINT       PRIMARY KEY,
    visibility             VARCHAR(32)  NOT NULL DEFAULT 'PRIVATE',
    enterprise_id          BIGINT,
    title                  VARCHAR(128) NOT NULL,
    description            TEXT         NOT NULL,
    time_limit_ms          INT          NOT NULL DEFAULT 1000,
    memory_limit_mb        INT          NOT NULL DEFAULT 256,
    supported_languages    JSONB,
    version                INT          NOT NULL DEFAULT 0,
    is_deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by             BIGINT,
    trace_id               VARCHAR(128),
    CONSTRAINT chk_code_questions_visibility
        CHECK (visibility IN ('GLOBAL', 'PRIVATE')),
    CONSTRAINT chk_code_questions_tenant
        CHECK (
            (visibility = 'GLOBAL' AND enterprise_id IS NULL)
            OR (visibility = 'PRIVATE' AND enterprise_id IS NOT NULL)
        ),
    CONSTRAINT chk_code_questions_time_limit
        CHECK (time_limit_ms BETWEEN 100 AND 60000),
    CONSTRAINT chk_code_questions_memory_limit
        CHECK (memory_limit_mb BETWEEN 16 AND 1024),
    CONSTRAINT chk_code_questions_version
        CHECK (version >= 0),
    CONSTRAINT chk_code_questions_languages_json
        CHECK (
            supported_languages IS NULL
            OR JSONB_TYPEOF(supported_languages) = 'array'
        )
);

COMMENT ON TABLE code_questions IS '算法与编程题库；GLOBAL 为平台公共题，PRIVATE 为企业私有题';
COMMENT ON COLUMN code_questions.id IS '雪花主键';
COMMENT ON COLUMN code_questions.visibility IS 'GLOBAL (公共题库) / PRIVATE (企业私有)';
COMMENT ON COLUMN code_questions.enterprise_id IS '逻辑关联 enterprises.id，GLOBAL 时为 NULL';
COMMENT ON COLUMN code_questions.supported_languages IS '允许提交的编程语言列表（JSONB 数组）';
COMMENT ON COLUMN code_questions.version IS '乐观锁版本号，管理端编辑防覆盖';

CREATE INDEX IF NOT EXISTS idx_code_questions_enterprise
    ON code_questions (enterprise_id, id DESC)
    WHERE is_deleted = FALSE AND visibility = 'PRIVATE';

CREATE INDEX IF NOT EXISTS idx_code_questions_global
    ON code_questions (id DESC)
    WHERE is_deleted = FALSE AND visibility = 'GLOBAL';

-- 算法题目关联评测测试用例表
CREATE TABLE IF NOT EXISTS code_test_cases
(
    id              BIGINT      PRIMARY KEY,
    question_id     BIGINT      NOT NULL,
    input_case      TEXT        NOT NULL,
    expected_output TEXT        NOT NULL,
    is_secret       BOOLEAN     NOT NULL DEFAULT FALSE,
    version         INT         NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT,
    trace_id        VARCHAR(128),
    CONSTRAINT chk_code_test_cases_version
        CHECK (version >= 0)
);

COMMENT ON TABLE code_test_cases IS '算法题目关联评测测试用例；隐藏用例不向候选人下发';
COMMENT ON COLUMN code_test_cases.id IS '雪花主键';
COMMENT ON COLUMN code_test_cases.question_id IS '逻辑关联 code_questions.id';
COMMENT ON COLUMN code_test_cases.is_secret IS '是否为隐藏黑盒边界用例';
COMMENT ON COLUMN code_test_cases.version IS '乐观锁版本号，管理端编辑防覆盖';

CREATE INDEX IF NOT EXISTS idx_code_test_cases_question
    ON code_test_cases (question_id, id)
    WHERE is_deleted = FALSE;

-- 代码提交与运行记录表
CREATE TABLE IF NOT EXISTS code_submissions
(
    id               BIGINT       PRIMARY KEY,
    session_id       BIGINT       NOT NULL,
    enterprise_id    BIGINT       NOT NULL,
    question_id      BIGINT       NOT NULL,
    language         VARCHAR(32)  NOT NULL,
    submitted_code   TEXT         NOT NULL,
    execution_status VARCHAR(32),
    execution_time_ms INT,
    memory_used_mb   INT,
    ai_review_json   JSONB,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    trace_id         VARCHAR(128),
    CONSTRAINT chk_code_submissions_status
        CHECK (
            execution_status IS NULL
            OR execution_status IN ('QUEUED', 'RUNNING', 'PASS', 'FAIL', 'TIMEOUT', 'ERROR')
        ),
    CONSTRAINT chk_code_submissions_time
        CHECK (execution_time_ms IS NULL OR execution_time_ms >= 0),
    CONSTRAINT chk_code_submissions_memory
        CHECK (memory_used_mb IS NULL OR memory_used_mb >= 0),
    CONSTRAINT chk_code_submissions_review_json
        CHECK (
            ai_review_json IS NULL
            OR JSONB_TYPEOF(ai_review_json) = 'object'
        )
);

COMMENT ON TABLE code_submissions IS '代码提交与沙箱运行记录；提交为异步评测，状态由执行器回填';
COMMENT ON COLUMN code_submissions.id IS '雪花主键';
COMMENT ON COLUMN code_submissions.session_id IS '逻辑关联 interview_sessions.id';
COMMENT ON COLUMN code_submissions.enterprise_id IS '逻辑关联 enterprises.id，租户隔离键';
COMMENT ON COLUMN code_submissions.question_id IS '逻辑关联 code_questions.id';
COMMENT ON COLUMN code_submissions.execution_status IS 'QUEUED/RUNNING/PASS/FAIL/TIMEOUT/ERROR';
COMMENT ON COLUMN code_submissions.ai_review_json IS 'AI 静态审查与重构建议（JSON 对象）';

CREATE INDEX IF NOT EXISTS idx_code_submissions_session
    ON code_submissions (session_id, id DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_code_submissions_enterprise_created
    ON code_submissions (enterprise_id, created_at DESC, id DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_code_submissions_session_question
    ON code_submissions (session_id, question_id, id)
    WHERE is_deleted = FALSE;

-- 逐测试用例执行结果表
CREATE TABLE IF NOT EXISTS code_submission_results
(
    id               BIGINT      PRIMARY KEY,
    submission_id    BIGINT      NOT NULL,
    test_case_id     BIGINT      NOT NULL,
    passed           BOOLEAN     NOT NULL,
    actual_output    TEXT,
    execution_time_ms INT,
    memory_used_mb   INT,
    trace_id         VARCHAR(128),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted       BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_code_submission_results_time
        CHECK (execution_time_ms IS NULL OR execution_time_ms >= 0),
    CONSTRAINT chk_code_submission_results_memory
        CHECK (memory_used_mb IS NULL OR memory_used_mb >= 0)
);

COMMENT ON TABLE code_submission_results IS '逐测试用例执行结果；由沙箱执行器写入';
COMMENT ON COLUMN code_submission_results.id IS '雪花主键';
COMMENT ON COLUMN code_submission_results.submission_id IS '逻辑关联 code_submissions.id';
COMMENT ON COLUMN code_submission_results.test_case_id IS '逻辑关联 code_test_cases.id';

CREATE INDEX IF NOT EXISTS idx_code_submission_results_submission
    ON code_submission_results (submission_id, test_case_id);

COMMIT;

-- >>>>>>>>>> 并入自: sql/7/phase7-sys_permissions-init.sql <<<<<<<<<<
-- =============================================
-- Phase 7: sys_permissions + sys_role_permissions 初始化
-- =============================================
-- 注意：sys_role_permissions 无 id 字段，主键为 (role_id, permission_id)

-- ===== sys_permissions =====

INSERT INTO sys_permissions (id, perm_code, perm_type, api_path, status) VALUES
-- 7.1 知识库文档管理
(901, 'knowledge-base:create',    'API', '/api/v1/enterprises/*/knowledge-bases', 1),
(902, 'knowledge-base:list',      'API', '/api/v1/enterprises/*/knowledge-bases', 1),
(903, 'knowledge-base:detail',    'API', '/api/v1/enterprises/*/knowledge-bases/*', 1),
(904, 'knowledge-base:delete',    'API', '/api/v1/enterprises/*/knowledge-bases/*', 1),
(905, 'knowledge-base:vectorize', 'API', '/api/v1/enterprises/*/knowledge-bases/*/vectorize', 1),

-- 7.2 RAG 对话
(911, 'rag:session-create',       'API', '/api/v1/rag/sessions', 1),
(912, 'rag:session-list',         'API', '/api/v1/rag/sessions', 1),
(913, 'rag:message-send',         'API', '/api/v1/rag/sessions/*/messages', 1),
(914, 'rag:message-list',         'API', '/api/v1/rag/sessions/*/messages', 1),
(915, 'rag:session-knowledge-bind','API', '/api/v1/rag/sessions/*/knowledge-bind', 1)
ON CONFLICT (id) DO UPDATE SET
    perm_code = EXCLUDED.perm_code,
    perm_type = EXCLUDED.perm_type,
    api_path = EXCLUDED.api_path,
    status = EXCLUDED.status;

-- ===== sys_role_permissions =====

-- SUPER_ADMIN (1001) — 全部 10 个权限
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(1001, 901, NOW()), (1001, 902, NOW()), (1001, 903, NOW()), (1001, 904, NOW()), (1001, 905, NOW()),
(1001, 911, NOW()), (1001, 912, NOW()), (1001, 913, NOW()), (1001, 914, NOW()), (1001, 915, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- FINANCE_ADMIN (1002) — 无知识库权限

-- ENTERPRISE_OWNER (2001) — 知识库全部 + RAG 全部
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2001, 901, NOW()), (2001, 902, NOW()), (2001, 903, NOW()), (2001, 904, NOW()), (2001, 905, NOW()),
(2001, 911, NOW()), (2001, 912, NOW()), (2001, 913, NOW()), (2001, 914, NOW()), (2001, 915, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ENTERPRISE_ADMIN (2002) — 与 OWNER 一致
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2002, 901, NOW()), (2002, 902, NOW()), (2002, 903, NOW()), (2002, 904, NOW()), (2002, 905, NOW()),
(2002, 911, NOW()), (2002, 912, NOW()), (2002, 913, NOW()), (2002, 914, NOW()), (2002, 915, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- HR_MANAGER (2003) — 知识库全部 + RAG 全部
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2003, 901, NOW()), (2003, 902, NOW()), (2003, 903, NOW()), (2003, 904, NOW()), (2003, 905, NOW()),
(2003, 911, NOW()), (2003, 912, NOW()), (2003, 913, NOW()), (2003, 914, NOW()), (2003, 915, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- HR_RECRUITER (2004) — 知识库查看/上传 + RAG 使用
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2004, 901, NOW()), (2004, 902, NOW()), (2004, 903, NOW()),
(2004, 911, NOW()), (2004, 912, NOW()), (2004, 913, NOW()), (2004, 914, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- INTERVIEWER (2005) — 知识库查看 + RAG 使用
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2005, 902, NOW()), (2005, 903, NOW()),
(2005, 911, NOW()), (2005, 912, NOW()), (2005, 913, NOW()), (2005, 914, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- CANDIDATE (3001) — RAG 使用（面试中检索知识库）
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(3001, 911, NOW()), (3001, 912, NOW()), (3001, 913, NOW()), (3001, 914, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- >>>>>>>>>> 并入自: sql/7/phase7-tables-init.sql <<<<<<<<<<
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

-- >>>>>>>>>> 并入自: sql/8/phase8-sys_permissions-init.sql <<<<<<<<<<
-- =============================================
-- Phase 8: sys_permissions + sys_role_permissions 初始化（风控与合规增强）
-- =============================================
-- 注意：sys_role_permissions 无 id 字段，主键为 (role_id, permission_id)

-- ===== sys_permissions =====

INSERT INTO sys_permissions (id, perm_code, perm_type, api_path, status) VALUES
-- 8.1 个人实名认证 (KYC)
(1001, 'kyc:submit', 'API', '/api/v1/candidate/kyc', 1),
(1002, 'kyc:status', 'API', '/api/v1/candidate/kyc/status', 1),

-- 8.1 企业资质认证
(1005, 'enterprise:cert:submit', 'API', '/api/v1/enterprises/*/certification', 1),
(1006, 'enterprise:cert:status', 'API', '/api/v1/enterprises/*/certification/status', 1),

-- 8.1 Admin KYC 审核
(1003, 'admin:kyc:audit', 'API', '/api/v1/admin/kyc/*/audit', 1),
(1004, 'admin:kyc:list',   'API', '/api/v1/admin/kyc', 1),

-- 8.1 Admin 企业认证审核
(1007, 'admin:cert:audit', 'API', '/api/v1/admin/cert/*/audit', 1),
(1008, 'admin:cert:list',   'API', '/api/v1/admin/cert', 1),

-- 8.2 敏感词管理
(1009, 'admin:sensitive-words:list',   'API', '/api/v1/admin/sensitive-words', 1),
(1010, 'admin:sensitive-words:create', 'API', '/api/v1/admin/sensitive-words', 1),
(1011, 'admin:sensitive-words:update', 'API', '/api/v1/admin/sensitive-words/*', 1),
(1012, 'admin:sensitive-words:delete', 'API', '/api/v1/admin/sensitive-words/*', 1),

-- 8.3 防作弊日志
(1013, 'admin:anti-cheat:list',   'API', '/api/v1/admin/anti-cheat-logs', 1),
(1014, 'admin:anti-cheat:detail', 'API', '/api/v1/admin/anti-cheat-logs/*', 1),

-- 8.4 API 策略管理
(1015, 'admin:api-policies:create', 'API', '/api/v1/admin/api-policies', 1),
(1016, 'admin:api-policies:list',   'API', '/api/v1/admin/api-policies', 1),
(1017, 'admin:api-policies:update', 'API', '/api/v1/admin/api-policies/*', 1),
(1018, 'admin:api-policies:delete', 'API', '/api/v1/admin/api-policies/*', 1)
ON CONFLICT (id) DO UPDATE SET
    perm_code = EXCLUDED.perm_code,
    perm_type = EXCLUDED.perm_type,
    api_path = EXCLUDED.api_path,
    status = EXCLUDED.status;

-- ===== sys_role_permissions =====

-- SUPER_ADMIN (1001) — 全部 Phase 8 权限
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(1001, 1001, NOW()), (1001, 1002, NOW()), (1001, 1003, NOW()), (1001, 1004, NOW()),
(1001, 1005, NOW()), (1001, 1006, NOW()), (1001, 1007, NOW()), (1001, 1008, NOW()),
(1001, 1009, NOW()), (1001, 1010, NOW()), (1001, 1011, NOW()), (1001, 1012, NOW()),
(1001, 1013, NOW()), (1001, 1014, NOW()),
(1001, 1015, NOW()), (1001, 1016, NOW()), (1001, 1017, NOW()), (1001, 1018, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ENTERPRISE_OWNER (2001) — 企业认证提交/查询
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2001, 1005, NOW()), (2001, 1006, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ENTERPRISE_ADMIN (2002) — 企业认证提交/查询
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2002, 1005, NOW()), (2002, 1006, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- CANDIDATE (3001) — 个人实名认证提交/查询
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(3001, 1001, NOW()), (3001, 1002, NOW())
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- >>>>>>>>>> 并入自: sql/8/phase8-tables-init.sql <<<<<<<<<<
-- 第八阶段：风控与合规增强表结构初始化
-- PostgreSQL；跨模块关联仅使用逻辑外键，不创建物理 FOREIGN KEY。
-- 依据 docs/第8阶段/第八阶段数据库表设计.md 与 docs/databases.md 生成。
--
-- 说明：
-- 1. sys_user_kyc / sys_enterprise_cert 不建通用 version 列（databases.md 版本策略：
--    依据期望状态完成原子更新），审核 CAS 以 auth_status/audit_status = 待审核 为条件。
-- 2. sensitive_words / user_api_policies 为多管理员可编辑配置聚合，按 databases.md
--    版本策略建议增加 version INT NOT NULL DEFAULT 0，编辑/删除使用 expectedVersion 条件更新。
-- 3. 唯一约束统一挂在未删除行上（部分唯一索引），支持逻辑删除后重新提交/重建。
-- 4. anti_cheat_logs 客户端事件幂等：UNIQUE(session_id, client_event_id) 部分唯一索引，
--    批量上报接口落地后以此去重（本阶段仅建表）。
-- 5. sys_enterprise_cert 在表设计文档基础上增加 reject_reason VARCHAR(256)：
--    接口契约要求认证状态/详情返回拒绝原因（REJECT 审核必填原因），不落库无处承载。

BEGIN;

-- C 端个人实名认证表
CREATE TABLE IF NOT EXISTS sys_user_kyc
(
    id            BIGINT       PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    real_name     VARCHAR(64)  NOT NULL,
    id_card_no    VARCHAR(128) NOT NULL,
    face_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    auth_status   SMALLINT     NOT NULL DEFAULT 0,
    reject_reason VARCHAR(256),
    submit_time   TIMESTAMPTZ  NOT NULL,
    audit_time    TIMESTAMPTZ,
    is_deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    trace_id      VARCHAR(128),
    updated_at    TIMESTAMPTZ,
    CONSTRAINT chk_sys_user_kyc_auth_status
        CHECK (auth_status IN (0, 1, 2))
);

COMMENT ON TABLE sys_user_kyc IS 'C 端个人实名认证表；real_name/id_card_no 由应用层 AES 加密存储';
COMMENT ON COLUMN sys_user_kyc.id IS '雪花主键';
COMMENT ON COLUMN sys_user_kyc.user_id IS '逻辑关联 sys_users.id，关联的 C 端求职者（未删除行唯一）';
COMMENT ON COLUMN sys_user_kyc.real_name IS '真实姓名（密文存储）';
COMMENT ON COLUMN sys_user_kyc.id_card_no IS '身份证号（密文存储，未删除行唯一）';
COMMENT ON COLUMN sys_user_kyc.face_verified IS '活体人脸核验状态';
COMMENT ON COLUMN sys_user_kyc.auth_status IS '综合认证状态：0 待审核 / 1 已通过 / 2 已拒绝';
COMMENT ON COLUMN sys_user_kyc.reject_reason IS '审核拒绝原因';
COMMENT ON COLUMN sys_user_kyc.submit_time IS '首次/最近一次提交时间';
COMMENT ON COLUMN sys_user_kyc.audit_time IS '最终审核处理时间';

-- 一个用户仅一条有效实名记录；逻辑删除后可重新提交
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_kyc_user
    ON sys_user_kyc (user_id)
    WHERE is_deleted = FALSE;

-- 身份证号未删除行唯一（密文等值比较）
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_kyc_id_card
    ON sys_user_kyc (id_card_no)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_sys_user_kyc_status
    ON sys_user_kyc (auth_status, submit_time DESC)
    WHERE is_deleted = FALSE;

-- B 端企业主体资质认证表
CREATE TABLE IF NOT EXISTS sys_enterprise_cert
(
    id            BIGINT       PRIMARY KEY,
    enterprise_id BIGINT       NOT NULL,
    company_name  VARCHAR(128) NOT NULL,
    credit_code   VARCHAR(64)  NOT NULL,
    legal_person  VARCHAR(64)  NOT NULL,
    license_url   VARCHAR(512) NOT NULL,
    audit_status  SMALLINT     NOT NULL DEFAULT 0,
    reject_reason VARCHAR(256),
    auditor_id    BIGINT,
    submit_time   TIMESTAMPTZ  NOT NULL,
    audit_time    TIMESTAMPTZ,
    is_deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    trace_id      VARCHAR(128),
    updated_at    TIMESTAMPTZ,
    CONSTRAINT chk_sys_enterprise_cert_audit_status
        CHECK (audit_status IN (0, 1, 2))
);

COMMENT ON TABLE sys_enterprise_cert IS 'B 端企业主体资质认证表；company_name 为提交时从 enterprises 读取的快照';
COMMENT ON COLUMN sys_enterprise_cert.id IS '雪花主键';
COMMENT ON COLUMN sys_enterprise_cert.enterprise_id IS '逻辑关联 enterprises.id，未删除行唯一';
COMMENT ON COLUMN sys_enterprise_cert.credit_code IS '统一社会信用代码，未删除行唯一';
COMMENT ON COLUMN sys_enterprise_cert.legal_person IS '法定代表人姓名';
COMMENT ON COLUMN sys_enterprise_cert.license_url IS '营业执照扫描件存储路径（上传凭证签发的 materialToken）';
COMMENT ON COLUMN sys_enterprise_cert.audit_status IS '审核状态：0 待审核 / 1 已通过 / 2 已拒绝';
COMMENT ON COLUMN sys_enterprise_cert.reject_reason IS '审核拒绝原因（接口契约要求返回，表设计补充列）';
COMMENT ON COLUMN sys_enterprise_cert.auditor_id IS '逻辑关联 sys_users.id，平台审核专员';
COMMENT ON COLUMN sys_enterprise_cert.submit_time IS '资料提交时间';
COMMENT ON COLUMN sys_enterprise_cert.audit_time IS '审核处理时间';

-- 一个企业仅一条有效认证记录；拒绝后覆盖重报，逻辑删除后可重建
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_enterprise_cert_enterprise
    ON sys_enterprise_cert (enterprise_id)
    WHERE is_deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_enterprise_cert_credit_code
    ON sys_enterprise_cert (credit_code)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_sys_enterprise_cert_status
    ON sys_enterprise_cert (audit_status, submit_time DESC)
    WHERE is_deleted = FALSE;

-- 违规敏感词与防线配置表
CREATE TABLE IF NOT EXISTS sensitive_words
(
    id         BIGINT       PRIMARY KEY,
    word       VARCHAR(64)  NOT NULL,
    category   VARCHAR(32)  NOT NULL,
    action_type VARCHAR(16) NOT NULL DEFAULT 'BLOCK',
    version    INT          NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_by BIGINT,
    trace_id   VARCHAR(128),
    updated_at TIMESTAMPTZ,
    CONSTRAINT chk_sensitive_words_category
        CHECK (category IN ('POLITICAL', 'PROFANITY', 'CHEAT')),
    CONSTRAINT chk_sensitive_words_action_type
        CHECK (action_type IN ('BLOCK', 'REPLACE', 'ALERT')),
    CONSTRAINT chk_sensitive_words_version
        CHECK (version >= 0)
);

COMMENT ON TABLE sensitive_words IS '违规敏感词与防线配置表；管理端编辑/删除使用 expectedVersion 乐观锁';
COMMENT ON COLUMN sensitive_words.id IS '雪花主键';
COMMENT ON COLUMN sensitive_words.word IS '敏感词汇本体，未删除行唯一';
COMMENT ON COLUMN sensitive_words.category IS '类别：POLITICAL / PROFANITY / CHEAT';
COMMENT ON COLUMN sensitive_words.action_type IS '触发动作：BLOCK 拦截 / REPLACE 替换 / ALERT 告警';
COMMENT ON COLUMN sensitive_words.version IS '乐观锁版本号';
COMMENT ON COLUMN sensitive_words.updated_by IS '逻辑关联 sys_users.id，维护词库的运营 ID';

-- 逻辑删除后允许同词重建
CREATE UNIQUE INDEX IF NOT EXISTS uk_sensitive_words_word
    ON sensitive_words (word)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_sensitive_words_category
    ON sensitive_words (category, id DESC)
    WHERE is_deleted = FALSE;

-- 用户 API 策略表（限流/黑白名单）
CREATE TABLE IF NOT EXISTS user_api_policies
(
    id             BIGINT       PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    api_path_regex VARCHAR(256),
    policy_type    VARCHAR(32)  NOT NULL,
    limit_count    INT,
    limit_seconds  INT,
    action_type    SMALLINT     NOT NULL,
    expire_time    TIMESTAMPTZ,
    reason         VARCHAR(128),
    version        INT          NOT NULL DEFAULT 0,
    is_deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_user_api_policies_policy_type
        CHECK (policy_type IN ('RATE_LIMIT', 'BLACKLIST', 'WHITELIST')),
    CONSTRAINT chk_user_api_policies_action_type
        CHECK (action_type IN (1, -1)),
    CONSTRAINT chk_user_api_policies_version
        CHECK (version >= 0),
    CONSTRAINT chk_user_api_policies_rate_limit_params
        CHECK (
            (policy_type = 'RATE_LIMIT' AND limit_count IS NOT NULL AND limit_count > 0
                AND limit_seconds IS NOT NULL AND limit_seconds > 0)
            OR (policy_type IN ('BLACKLIST', 'WHITELIST')
                AND limit_count IS NULL AND limit_seconds IS NULL)
        )
);

COMMENT ON TABLE user_api_policies IS '用户 API 策略表（限流/黑白名单）；api_path_regex 为受限 Ant 风格模板，空表示匹配全部';
COMMENT ON COLUMN user_api_policies.id IS '雪花主键';
COMMENT ON COLUMN user_api_policies.user_id IS '逻辑关联 sys_users.id，被管控用户';
COMMENT ON COLUMN user_api_policies.policy_type IS '策略类型：RATE_LIMIT / BLACKLIST / WHITELIST';
COMMENT ON COLUMN user_api_policies.limit_count IS '时间窗口内允许次数（仅限流）';
COMMENT ON COLUMN user_api_policies.limit_seconds IS '时间窗口秒数（仅限流）';
COMMENT ON COLUMN user_api_policies.action_type IS '动作：1 强制放行 / -1 强制拦截';
COMMENT ON COLUMN user_api_policies.expire_time IS '策略过期时间，为空则永久';
COMMENT ON COLUMN user_api_policies.reason IS '风控原因备注';
COMMENT ON COLUMN user_api_policies.version IS '乐观锁版本号';

CREATE INDEX IF NOT EXISTS idx_user_api_policies_user
    ON user_api_policies (user_id, id DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_user_api_policies_type
    ON user_api_policies (policy_type, id DESC)
    WHERE is_deleted = FALSE;

-- 智能防作弊行为检测日志表
CREATE TABLE IF NOT EXISTS anti_cheat_logs
(
    id               BIGINT       PRIMARY KEY,
    enterprise_id    BIGINT       NOT NULL,
    session_id       BIGINT       NOT NULL,
    user_id          BIGINT       NOT NULL,
    client_event_id  VARCHAR(64)  NOT NULL,
    event_type       VARCHAR(32)  NOT NULL,
    duration_ms      BIGINT,
    snapshot_oss_url VARCHAR(512),
    trace_id         VARCHAR(128),
    is_deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_anti_cheat_logs_event_type
        CHECK (event_type IN ('PAGE_BLUR', 'NO_FACE', 'MULTI_FACE'))
);

COMMENT ON TABLE anti_cheat_logs IS '智能防作弊行为检测日志表；客户端信号仅为风险信号，不单独作为处罚结论';
COMMENT ON COLUMN anti_cheat_logs.id IS '雪花主键';
COMMENT ON COLUMN anti_cheat_logs.enterprise_id IS '逻辑关联 enterprises.id';
COMMENT ON COLUMN anti_cheat_logs.session_id IS '逻辑关联 interview_sessions.id（多态：文本/语音会话）';
COMMENT ON COLUMN anti_cheat_logs.user_id IS '逻辑关联 sys_users.id，涉嫌异常的候选人';
COMMENT ON COLUMN anti_cheat_logs.client_event_id IS '客户端事件幂等 ID，(session_id, client_event_id) 去重';
COMMENT ON COLUMN anti_cheat_logs.event_type IS '违规类型：PAGE_BLUR / NO_FACE / MULTI_FACE';
COMMENT ON COLUMN anti_cheat_logs.duration_ms IS '异常行为持续时间（毫秒）';
COMMENT ON COLUMN anti_cheat_logs.snapshot_oss_url IS '抓拍的现场暗灰度图像存储路径';
COMMENT ON COLUMN anti_cheat_logs.created_at IS '违规发生时间';

-- 客户端事件幂等：同一会话同一 eventId 仅记录一次
CREATE UNIQUE INDEX IF NOT EXISTS uk_anti_cheat_logs_session_event
    ON anti_cheat_logs (session_id, client_event_id)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_anti_cheat_logs_enterprise_time
    ON anti_cheat_logs (enterprise_id, created_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_anti_cheat_logs_filters
    ON anti_cheat_logs (event_type, user_id, session_id, created_at DESC)
    WHERE is_deleted = FALSE;

COMMIT;

-- >>>>>>>>>> 并入自: sql/9/phase9-sys_permissions-init.sql <<<<<<<<<<
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

-- >>>>>>>>>> 并入自: sql/9/phase9-tables-init.sql <<<<<<<<<<
-- ============================================================
-- Phase 9: 增值与运营 — 表结构初始化
-- 数据库: PostgreSQL 14+；幂等可重复执行；不使用物理外键；
-- 时间字段使用 TIMESTAMPTZ，布尔使用 BOOLEAN，JSON 使用 JSONB。
-- ============================================================

BEGIN;

-- ==================== 9.1 interview_calendar_slots ====================
-- 面试官日历空闲时段表；version 支撑 If-Match 条件删除，
-- created_at/updated_by/trace_id 为接口契约要求的审计字段。
CREATE TABLE IF NOT EXISTS interview_calendar_slots
(
    id                 BIGINT       PRIMARY KEY,
    enterprise_id      BIGINT       NOT NULL,
    interviewer_user_id BIGINT      NOT NULL,
    slot_start         TIMESTAMPTZ  NOT NULL,
    slot_end           TIMESTAMPTZ  NOT NULL,
    is_allocated       BOOLEAN      NOT NULL DEFAULT FALSE,
    version            INT          NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by         BIGINT,
    trace_id           VARCHAR(128),
    is_deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_calendar_slots_time CHECK (slot_start < slot_end)
);

COMMENT ON TABLE interview_calendar_slots IS '面试官日历空闲时段表';
COMMENT ON COLUMN interview_calendar_slots.id IS '雪花主键';
COMMENT ON COLUMN interview_calendar_slots.enterprise_id IS '[逻辑外键]→enterprises，关联企业租户 ID';
COMMENT ON COLUMN interview_calendar_slots.interviewer_user_id IS '[逻辑外键]→sys_users，面试官系统用户 ID';
COMMENT ON COLUMN interview_calendar_slots.slot_start IS '空闲时段开始时间';
COMMENT ON COLUMN interview_calendar_slots.slot_end IS '空闲时段结束时间';
COMMENT ON COLUMN interview_calendar_slots.is_allocated IS 'FALSE: 空闲开放中, TRUE: 已排期锁定';
COMMENT ON COLUMN interview_calendar_slots.version IS 'CAS 版本号；If-Match 条件删除使用';
COMMENT ON COLUMN interview_calendar_slots.created_at IS '创建时间';
COMMENT ON COLUMN interview_calendar_slots.updated_by IS '[逻辑外键]→sys_users，最后修改人';
COMMENT ON COLUMN interview_calendar_slots.trace_id IS '调用链 ID';
COMMENT ON COLUMN interview_calendar_slots.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN interview_calendar_slots.updated_at IS '更新时间';

CREATE INDEX IF NOT EXISTS idx_calendar_slots_interviewer_time
    ON interview_calendar_slots (interviewer_user_id, slot_start);
CREATE INDEX IF NOT EXISTS idx_calendar_slots_enterprise
    ON interview_calendar_slots (enterprise_id, slot_start);


-- ==================== 9.1 ai_tutor_sessions ====================
-- C 端智能考点答疑会话主表。
CREATE TABLE IF NOT EXISTS ai_tutor_sessions
(
    id                   BIGINT        PRIMARY KEY,
    enterprise_id        BIGINT        NOT NULL,
    user_id              BIGINT        NOT NULL,
    associated_report_id BIGINT        NOT NULL,
    session_title        VARCHAR(128)  NOT NULL,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trace_id             VARCHAR(128),
    is_deleted           BOOLEAN       NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE ai_tutor_sessions IS 'C 端智能考点答疑教研会话主表';
COMMENT ON COLUMN ai_tutor_sessions.id IS '雪花主键';
COMMENT ON COLUMN ai_tutor_sessions.enterprise_id IS '[逻辑外键]→enterprises，强隔离企业租户 ID';
COMMENT ON COLUMN ai_tutor_sessions.user_id IS '[逻辑外键]→sys_users，发起答疑的 C 端求职者 ID';
COMMENT ON COLUMN ai_tutor_sessions.associated_report_id IS '[逻辑外键]→interview_reports，强关联面评报告 ID';
COMMENT ON COLUMN ai_tutor_sessions.session_title IS '会话引导自拟标题';
COMMENT ON COLUMN ai_tutor_sessions.created_at IS '会话开启时间';
COMMENT ON COLUMN ai_tutor_sessions.updated_at IS '最后提问活跃时间';
COMMENT ON COLUMN ai_tutor_sessions.trace_id IS '会话创建调用链 ID';
COMMENT ON COLUMN ai_tutor_sessions.is_deleted IS '逻辑删除标识';

CREATE INDEX IF NOT EXISTS idx_tutor_sessions_user_time
    ON ai_tutor_sessions (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_tutor_sessions_report
    ON ai_tutor_sessions (associated_report_id);


-- ==================== 9.1 ai_tutor_messages ====================
-- AI 答疑交互消息表；用户消息与 AI 消息分开保存，id 单调递增支撑游标分页。
CREATE TABLE IF NOT EXISTS ai_tutor_messages
(
    id           BIGINT       PRIMARY KEY,
    session_id   BIGINT       NOT NULL,
    message_type VARCHAR(16)  NOT NULL,
    content      TEXT         NOT NULL,
    trace_id     VARCHAR(128),
    is_deleted   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_tutor_messages_type CHECK (message_type IN ('USER', 'AI')),
    CONSTRAINT chk_tutor_messages_content CHECK (NULLIF(BTRIM(content), '') IS NOT NULL)
);

COMMENT ON TABLE ai_tutor_messages IS 'AI 答疑交互消息表';
COMMENT ON COLUMN ai_tutor_messages.id IS '雪花主键；游标分页游标';
COMMENT ON COLUMN ai_tutor_messages.session_id IS '[逻辑外键]→ai_tutor_sessions，关联答疑会话';
COMMENT ON COLUMN ai_tutor_messages.message_type IS '消息角色 (USER / AI)';
COMMENT ON COLUMN ai_tutor_messages.content IS '提问或回答正文';
COMMENT ON COLUMN ai_tutor_messages.trace_id IS '本次问答调用链 ID';
COMMENT ON COLUMN ai_tutor_messages.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN ai_tutor_messages.created_at IS '消息创建时间';

CREATE INDEX IF NOT EXISTS idx_tutor_messages_session_id
    ON ai_tutor_messages (session_id, id);


-- ==================== 9.2 sys_notifications ====================
-- 系统消息与投递记录表（含 email_logs/sms_logs 功能）。
-- 若库中已存在旧结构，先按原结构建表（IF NOT EXISTS 不生效时跳过），
-- 再以 ALTER TABLE ... ADD COLUMN IF NOT EXISTS 补扩列。
CREATE TABLE IF NOT EXISTS sys_notifications
(
    id             BIGINT        PRIMARY KEY,
    enterprise_id  BIGINT        NOT NULL,
    user_id        BIGINT        NOT NULL,
    notify_type    VARCHAR(32)   NOT NULL,
    title          VARCHAR(128)  NOT NULL,
    content        TEXT,
    is_read        BOOLEAN       NOT NULL DEFAULT FALSE,
    trace_id       VARCHAR(128),
    is_deleted     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2026-08-21 接口回填扩列：发送侧管理接口依赖以下列。
ALTER TABLE sys_notifications ADD COLUMN IF NOT EXISTS notify_scene VARCHAR(64);
ALTER TABLE sys_notifications ADD COLUMN IF NOT EXISTS channel_type VARCHAR(16) NOT NULL DEFAULT 'IN_APP';
ALTER TABLE sys_notifications ADD COLUMN IF NOT EXISTS send_status VARCHAR(20) NOT NULL DEFAULT 'SENT';
ALTER TABLE sys_notifications ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(512);
ALTER TABLE sys_notifications ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 2026-09-06 发送幂等：防业务重试/并发产生重复站内信；流水行不带键不受约束。
ALTER TABLE sys_notifications ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS uk_notifications_idem
    ON sys_notifications (idempotency_key)
    WHERE idempotency_key IS NOT NULL AND is_deleted = FALSE;

COMMENT ON TABLE sys_notifications IS '系统消息与投递记录表（含 email_logs 功能）';
COMMENT ON COLUMN sys_notifications.id IS '雪花主键';
COMMENT ON COLUMN sys_notifications.enterprise_id IS '[逻辑外键]→enterprises，强隔离企业租户 ID';
COMMENT ON COLUMN sys_notifications.user_id IS '[逻辑外键]→sys_users，接收消息的用户 ID';
COMMENT ON COLUMN sys_notifications.notify_type IS '类型 (SYSTEM / INTERVIEW / EMAIL_LOG / SMS_LOG)';
COMMENT ON COLUMN sys_notifications.notify_scene IS '业务场景 (INTERVIEW_INVITE / INTERVIEW_CANCEL / OFFER_SENT / OFFER_DECIDED / REPORT_READY / SYSTEM)';
COMMENT ON COLUMN sys_notifications.channel_type IS '发送渠道 (IN_APP / EMAIL / SMS)；IN_APP 不依赖外部渠道配置';
COMMENT ON COLUMN sys_notifications.send_status IS '发送状态 (PENDING / SENT / FAILED)；存量站内信数据视为 SENT';
COMMENT ON COLUMN sys_notifications.failure_reason IS '外部渠道发送失败原因';
COMMENT ON COLUMN sys_notifications.idempotency_key IS '发送幂等键（{scene}:{bizId}）；唯一索引仅约束非空且未删除的行';
COMMENT ON COLUMN sys_notifications.title IS '消息/邮件标题（模板渲染后）';
COMMENT ON COLUMN sys_notifications.content IS '消息/邮件正文内容（模板渲染后）';
COMMENT ON COLUMN sys_notifications.is_read IS '已读/送达状态';
COMMENT ON COLUMN sys_notifications.trace_id IS '触发通知的原始业务链路 ID';
COMMENT ON COLUMN sys_notifications.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN sys_notifications.created_at IS '发送时间';
COMMENT ON COLUMN sys_notifications.updated_at IS '重发/发送状态流转时间';

CREATE INDEX IF NOT EXISTS idx_notifications_user_created
    ON sys_notifications (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_channel_status
    ON sys_notifications (channel_type, send_status, created_at DESC);


-- ==================== 9.2 sys_notification_channels ====================
-- 通知发送渠道配置表；每渠道固定一行，凭证字段在 config_json 内加密存储。
CREATE TABLE IF NOT EXISTS sys_notification_channels
(
    id           BIGINT       PRIMARY KEY,
    channel_type VARCHAR(16)  NOT NULL,
    is_enabled   BOOLEAN      NOT NULL DEFAULT FALSE,
    provider     VARCHAR(64),
    config_json  JSONB,
    version      INT          NOT NULL DEFAULT 0,
    updated_by   BIGINT,
    trace_id     VARCHAR(128),
    is_deleted   BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_notification_channels_type CHECK (channel_type IN ('EMAIL', 'SMS', 'IN_APP'))
);

COMMENT ON TABLE sys_notification_channels IS '通知发送渠道配置表';
COMMENT ON COLUMN sys_notification_channels.id IS '雪花主键';
COMMENT ON COLUMN sys_notification_channels.channel_type IS '渠道类型 (EMAIL / SMS / IN_APP)；每渠道固定一行';
COMMENT ON COLUMN sys_notification_channels.is_enabled IS '渠道启用开关；IN_APP 行恒为 TRUE';
COMMENT ON COLUMN sys_notification_channels.provider IS '供应商标识（如 ALIYUN_SMS）';
COMMENT ON COLUMN sys_notification_channels.config_json IS '渠道参数；凭证字段加密存储，查询接口仅返回脱敏摘要';
COMMENT ON COLUMN sys_notification_channels.version IS 'CAS 版本号；PUT 更新携带 expectedVersion，零行更新返回 409';
COMMENT ON COLUMN sys_notification_channels.updated_by IS '[逻辑外键]→sys_users，最后修改人';
COMMENT ON COLUMN sys_notification_channels.trace_id IS '调用链 ID';
COMMENT ON COLUMN sys_notification_channels.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN sys_notification_channels.updated_at IS '更新时间';

-- 有效记录唯一索引（排除逻辑删除行），兜底并发创建。
CREATE UNIQUE INDEX IF NOT EXISTS uq_notification_channels_type
    ON sys_notification_channels (channel_type) WHERE is_deleted = FALSE;

-- 三渠道固定一行初始化；IN_APP 站内信恒为启用。
-- 种子行使用小字面主键，避免与应用侧雪花 ID 冲突。
INSERT INTO sys_notification_channels (id, channel_type, is_enabled)
VALUES (900001, 'IN_APP', TRUE), (900002, 'EMAIL', FALSE), (900003, 'SMS', FALSE)
ON CONFLICT DO NOTHING;


-- ==================== 9.2 sys_notification_templates ====================
-- 通知模板表；同一 (notify_scene, channel_type) 有效记录唯一。
CREATE TABLE IF NOT EXISTS sys_notification_templates
(
    id                BIGINT       PRIMARY KEY,
    notify_scene      VARCHAR(64)  NOT NULL,
    channel_type      VARCHAR(16)  NOT NULL,
    title             VARCHAR(128) NOT NULL,
    content_template  TEXT         NOT NULL,
    is_enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    version           INT          NOT NULL DEFAULT 0,
    updated_by        BIGINT,
    trace_id          VARCHAR(128),
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_notification_templates IS '通知模板表';
COMMENT ON COLUMN sys_notification_templates.id IS '雪花主键';
COMMENT ON COLUMN sys_notification_templates.notify_scene IS '业务场景，取值同 sys_notifications.notify_scene';
COMMENT ON COLUMN sys_notification_templates.channel_type IS '渠道 (EMAIL / SMS / IN_APP)';
COMMENT ON COLUMN sys_notification_templates.title IS '标题模板';
COMMENT ON COLUMN sys_notification_templates.content_template IS '正文模板；禁止客户端提交渲染后全文';
COMMENT ON COLUMN sys_notification_templates.is_enabled IS '模板启停';
COMMENT ON COLUMN sys_notification_templates.version IS 'CAS 版本号；PATCH 更新携带 expectedVersion';
COMMENT ON COLUMN sys_notification_templates.updated_by IS '[逻辑外键]→sys_users，最后修改人';
COMMENT ON COLUMN sys_notification_templates.trace_id IS '调用链 ID';
COMMENT ON COLUMN sys_notification_templates.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN sys_notification_templates.created_at IS '创建时间';
COMMENT ON COLUMN sys_notification_templates.updated_at IS '更新时间';

-- 有效记录唯一索引（排除逻辑删除行），兜底并发创建。
CREATE UNIQUE INDEX IF NOT EXISTS uq_notification_templates_scene_channel
    ON sys_notification_templates (notify_scene, channel_type) WHERE is_deleted = FALSE;


-- ==================== 9.3 sys_api_logs / sys_api_logs_archive ====================
-- API 高频访问日志热表与同构归档冷表。
CREATE TABLE IF NOT EXISTS sys_api_logs
(
    id               BIGINT       PRIMARY KEY,
    trace_id         VARCHAR(128) NOT NULL,
    user_id          BIGINT,
    api_url          VARCHAR(256) NOT NULL,
    request_method   VARCHAR(10)  NOT NULL,
    client_ip        VARCHAR(45),
    execution_time   BIGINT,
    response_status  INT,
    error_msg        VARCHAR(512),
    llm_input_tokens INT,
    llm_output_tokens INT,
    llm_model        VARCHAR(64),
    is_deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_api_logs IS 'API 高频访问日志热表';
COMMENT ON COLUMN sys_api_logs.id IS '雪花主键';
COMMENT ON COLUMN sys_api_logs.trace_id IS '分布式链路追踪 ID';
COMMENT ON COLUMN sys_api_logs.user_id IS '[逻辑外键]→sys_users，触发请求的操作人 ID';
COMMENT ON COLUMN sys_api_logs.api_url IS '请求的接口路径';
COMMENT ON COLUMN sys_api_logs.request_method IS 'HTTP 动词';
COMMENT ON COLUMN sys_api_logs.client_ip IS '客户端真实 IP 地址';
COMMENT ON COLUMN sys_api_logs.execution_time IS '接口执行耗时（毫秒）';
COMMENT ON COLUMN sys_api_logs.response_status IS 'HTTP 响应状态码';
COMMENT ON COLUMN sys_api_logs.error_msg IS '若接口报错，记录简要异常信息（脱敏）';
COMMENT ON COLUMN sys_api_logs.llm_input_tokens IS 'LLM 输入 Token 数（成本归因）';
COMMENT ON COLUMN sys_api_logs.llm_output_tokens IS 'LLM 输出 Token 数（成本归因）';
COMMENT ON COLUMN sys_api_logs.llm_model IS '使用的模型名称';
COMMENT ON COLUMN sys_api_logs.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN sys_api_logs.created_at IS '日志生成时间';

CREATE INDEX IF NOT EXISTS idx_api_logs_created_at ON sys_api_logs (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_api_logs_user_time ON sys_api_logs (user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS sys_api_logs_archive
(
    id               BIGINT       PRIMARY KEY,
    trace_id         VARCHAR(128) NOT NULL,
    user_id          BIGINT,
    api_url          VARCHAR(256) NOT NULL,
    request_method   VARCHAR(10)  NOT NULL,
    client_ip        VARCHAR(45),
    execution_time   BIGINT,
    response_status  INT,
    error_msg        VARCHAR(512),
    llm_input_tokens INT,
    llm_output_tokens INT,
    llm_model        VARCHAR(64),
    is_deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_api_logs_archive IS '运行日志历史归档冷表（结构同 sys_api_logs）';

CREATE INDEX IF NOT EXISTS idx_archive_logs_created_at
    ON sys_api_logs_archive (created_at DESC);


-- ==================== 9.3 sys_operate_logs ====================
-- 业务操作审计日志表；数据由 DataAuditInterceptor 产生。
CREATE TABLE IF NOT EXISTS sys_operate_logs
(
    id             BIGINT       PRIMARY KEY,
    trace_id       VARCHAR(128),
    user_id        BIGINT       NOT NULL,
    module         VARCHAR(64)  NOT NULL,
    operate_type   VARCHAR(16)  NOT NULL,
    target_table   VARCHAR(64),
    target_id      BIGINT,
    old_value_json JSONB,
    new_value_json JSONB,
    is_deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_operate_logs_type
        CHECK (operate_type IN ('INSERT', 'UPDATE', 'DELETE', 'GRANT'))
);

COMMENT ON TABLE sys_operate_logs IS '业务操作审计日志表（JSON 快照审计）';
COMMENT ON COLUMN sys_operate_logs.id IS '雪花主键';
COMMENT ON COLUMN sys_operate_logs.trace_id IS '关联同一次 HTTP 请求的链路 ID';
COMMENT ON COLUMN sys_operate_logs.user_id IS '[逻辑外键]→sys_users，操作人 ID';
COMMENT ON COLUMN sys_operate_logs.module IS '业务模块';
COMMENT ON COLUMN sys_operate_logs.operate_type IS '操作类型 (INSERT / UPDATE / DELETE / GRANT)';
COMMENT ON COLUMN sys_operate_logs.target_table IS '被修改的目标数据库表名';
COMMENT ON COLUMN sys_operate_logs.target_id IS '被修改的目标记录主键 ID';
COMMENT ON COLUMN sys_operate_logs.old_value_json IS '修改前的数据快照 (JSON，脱敏)';
COMMENT ON COLUMN sys_operate_logs.new_value_json IS '修改后的数据快照 (JSON，脱敏)';
COMMENT ON COLUMN sys_operate_logs.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN sys_operate_logs.created_at IS '操作发生时间';

CREATE INDEX IF NOT EXISTS idx_operate_table_time
    ON sys_operate_logs (target_table, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_operate_user_time
    ON sys_operate_logs (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_operate_biz
    ON sys_operate_logs (target_table, target_id);


-- ==================== 9.4 spark_corpus_tasks ====================
-- Spark 离线语料同步任务表。
CREATE TABLE IF NOT EXISTS spark_corpus_tasks
(
    id                        BIGINT       PRIMARY KEY,
    task_no                   VARCHAR(64)  NOT NULL,
    source_path               VARCHAR(512) NOT NULL,
    raw_count                 INT,
    cleaned_count             INT,
    chunk_count               INT,
    status                    VARCHAR(20),
    vector_db_write_latency_ms INT,
    embedding_api_latency_ms  INT,
    started_at                TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted                BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_by                BIGINT,
    trace_id                  VARCHAR(128),
    updated_at                TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_spark_tasks_task_no UNIQUE (task_no)
);

COMMENT ON TABLE spark_corpus_tasks IS 'Spark 离线语料同步任务表';
COMMENT ON COLUMN spark_corpus_tasks.id IS '雪花主键';
COMMENT ON COLUMN spark_corpus_tasks.task_no IS '批次任务唯一编号';
COMMENT ON COLUMN spark_corpus_tasks.source_path IS '数据源路径';
COMMENT ON COLUMN spark_corpus_tasks.raw_count IS '原始采集文本总条数';
COMMENT ON COLUMN spark_corpus_tasks.cleaned_count IS '清洗去噪后的有效条数';
COMMENT ON COLUMN spark_corpus_tasks.chunk_count IS '切片并成功向量化入库的 Chunk 总数';
COMMENT ON COLUMN spark_corpus_tasks.status IS '任务状态 (PENDING / RUNNING / SUCCESS / FAILED / CANCELLED)';
COMMENT ON COLUMN spark_corpus_tasks.vector_db_write_latency_ms IS '向量写入平均耗时（毫秒）';
COMMENT ON COLUMN spark_corpus_tasks.embedding_api_latency_ms IS 'Embedding 调用平均耗时（毫秒）';
COMMENT ON COLUMN spark_corpus_tasks.started_at IS '任务启动时间';
COMMENT ON COLUMN spark_corpus_tasks.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN spark_corpus_tasks.updated_by IS '[逻辑外键]→sys_users，操作人 ID';
COMMENT ON COLUMN spark_corpus_tasks.trace_id IS '调用链 ID';
COMMENT ON COLUMN spark_corpus_tasks.updated_at IS '更新时间';

CREATE INDEX IF NOT EXISTS idx_spark_tasks_status_time
    ON spark_corpus_tasks (status, started_at DESC);


-- ==================== 9.3 data_retention_policies ====================
-- 数据保留策略单行配置表（文档未定义 DDL，按接口契约设计）。
CREATE TABLE IF NOT EXISTS data_retention_policies
(
    id           BIGINT      PRIMARY KEY,
    policies_json JSONB,
    version      INT         NOT NULL DEFAULT 0,
    updated_by   BIGINT,
    trace_id     VARCHAR(128),
    is_deleted   BOOLEAN     NOT NULL DEFAULT FALSE,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_data_retention_single_row CHECK (id = 1)
);

COMMENT ON TABLE data_retention_policies IS '数据保留策略单行配置表（id 恒为 1）';
COMMENT ON COLUMN data_retention_policies.id IS '固定主键 1';
COMMENT ON COLUMN data_retention_policies.policies_json IS '策略数组 [{resourceType, hotRetentionDays, archiveEnabled, coldRetentionDays}]';
COMMENT ON COLUMN data_retention_policies.version IS '聚合 CAS 版本号';
COMMENT ON COLUMN data_retention_policies.updated_by IS '[逻辑外键]→sys_users，最后修改人';
COMMENT ON COLUMN data_retention_policies.trace_id IS '调用链 ID';
COMMENT ON COLUMN data_retention_policies.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN data_retention_policies.updated_at IS '更新时间';

-- 默认策略初始化：全部资源热保留 30 天、关闭归档。
INSERT INTO data_retention_policies (id, policies_json, version)
VALUES (1, '[{"resourceType":"API_LOG","hotRetentionDays":30,"archiveEnabled":true,"coldRetentionDays":365},
            {"resourceType":"OPERATE_LOG","hotRetentionDays":90,"archiveEnabled":true,"coldRetentionDays":365},
            {"resourceType":"INTERVIEW_TIMELINE","hotRetentionDays":180,"archiveEnabled":false},
            {"resourceType":"VOICE_MEDIA","hotRetentionDays":180,"archiveEnabled":false},
            {"resourceType":"INTERVIEW_REPORT","hotRetentionDays":365,"archiveEnabled":false}]'::jsonb, 0)
ON CONFLICT (id) DO NOTHING;


-- ==================== 9.3 data_archive_tasks ====================
-- 数据归档任务表（文档未定义 DDL，按接口契约设计）。
CREATE TABLE IF NOT EXISTS data_archive_tasks
(
    id              BIGINT       PRIMARY KEY,
    resource_type   VARCHAR(32)  NOT NULL,
    before_time     TIMESTAMPTZ,
    status          VARCHAR(20)  NOT NULL,
    archived_count  BIGINT       NOT NULL DEFAULT 0,
    failure_reason  VARCHAR(512),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at    TIMESTAMPTZ,
    updated_by      BIGINT,
    trace_id        VARCHAR(128),
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_archive_tasks_resource
        CHECK (resource_type IN ('API_LOG', 'OPERATE_LOG', 'INTERVIEW_TIMELINE', 'VOICE_MEDIA', 'INTERVIEW_REPORT')),
    CONSTRAINT chk_archive_tasks_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

COMMENT ON TABLE data_archive_tasks IS '数据归档任务表';
COMMENT ON COLUMN data_archive_tasks.id IS '雪花主键';
COMMENT ON COLUMN data_archive_tasks.resource_type IS '资源类型 (API_LOG / OPERATE_LOG / INTERVIEW_TIMELINE / VOICE_MEDIA / INTERVIEW_REPORT)';
COMMENT ON COLUMN data_archive_tasks.before_time IS '归档该时间之前的数据';
COMMENT ON COLUMN data_archive_tasks.status IS '任务状态 (PENDING / PROCESSING / COMPLETED / FAILED / CANCELLED)';
COMMENT ON COLUMN data_archive_tasks.archived_count IS '已归档条数';
COMMENT ON COLUMN data_archive_tasks.failure_reason IS '失败原因';
COMMENT ON COLUMN data_archive_tasks.created_at IS '创建时间';
COMMENT ON COLUMN data_archive_tasks.completed_at IS '完成时间';
COMMENT ON COLUMN data_archive_tasks.updated_by IS '[逻辑外键]→sys_users，操作人 ID';
COMMENT ON COLUMN data_archive_tasks.trace_id IS '调用链 ID';
COMMENT ON COLUMN data_archive_tasks.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN data_archive_tasks.updated_at IS '更新时间';

CREATE INDEX IF NOT EXISTS idx_archive_tasks_resource_status
    ON data_archive_tasks (resource_type, status, created_at DESC);

COMMIT;
