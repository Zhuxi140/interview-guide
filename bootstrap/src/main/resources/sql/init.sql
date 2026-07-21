-- ============================================================
-- 智能 SaaS 招聘与多模态面试平台 —— 全量 DDL + 权限种子
-- 数据库: PostgreSQL 16+
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
    status          SMALLINT        DEFAULT 1,
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
COMMENT ON COLUMN sys_users.user_type IS '用户类型 (PLATFORM_ADMIN / PLATFORM_OPS / HR / CANDIDATE)';
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

CREATE UNIQUE INDEX IF NOT EXISTS idx_user_role ON sys_user_roles (user_id, role_id);


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

CREATE UNIQUE INDEX IF NOT EXISTS idx_role_permission ON sys_role_permissions (role_id, permission_id);


-- ==================== 7. enterprises ====================
CREATE TABLE IF NOT EXISTS enterprises (
    id              BIGINT          NOT NULL,
    name            VARCHAR(128)    NOT NULL,
    short_name      VARCHAR(64)     NOT NULL,
    industry        VARCHAR(64)     NOT NULL,
    scale           VARCHAR(32),
    contact_email   VARCHAR(128)    NOT NULL,
    contact_phone   VARCHAR(20)     NOT NULL,
    status          SMALLINT        NOT NULL    DEFAULT 1,
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
    experience_req  VARCHAR(32),
    education_req   VARCHAR(32),
    skills_json     JSONB,
    status          SMALLINT        DEFAULT 1,
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
COMMENT ON COLUMN jobs.experience_req IS '经验要求';
COMMENT ON COLUMN jobs.education_req IS '学历要求';
COMMENT ON COLUMN jobs.skills_json IS '技能标签 JSON 数组';
COMMENT ON COLUMN jobs.status IS '岗位状态 (1: 开放中, 0: 已关闭)';
COMMENT ON COLUMN jobs.created_at IS '发布时间';
COMMENT ON COLUMN jobs.is_deleted IS '逻辑删除标识';
COMMENT ON COLUMN jobs.updated_by IS '轻量审计：操作人';
COMMENT ON COLUMN jobs.trace_id IS '调用链 ID';
COMMENT ON COLUMN jobs.updated_at IS '更新时间';


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
COMMENT ON COLUMN resumes.created_at IS '上传时间';
COMMENT ON COLUMN resumes.is_deleted IS '逻辑删除';
COMMENT ON COLUMN resumes.updated_by IS '[逻辑外键]→sys_users';
COMMENT ON COLUMN resumes.trace_id IS '触发解析的调用链 ID';
COMMENT ON COLUMN resumes.updated_at IS '最后更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS idx_resumes_file_hash ON resumes (user_id, file_hash);
CREATE INDEX IF NOT EXISTS idx_resumes_user_id ON resumes (user_id);


-- ==================== 11. resume_analyses ====================
CREATE TABLE IF NOT EXISTS resume_analyses (
    id              BIGINT          NOT NULL,
    resume_id       BIGINT          NOT NULL,
    overall_score   INT,
    strengths_json  JSONB,
    suggestions_json JSONB,
    analyzed_at     TIMESTAMPTZ     NOT NULL,
    is_deleted      BOOLEAN         DEFAULT FALSE,
    trace_id        VARCHAR(128),
    created_at      TIMESTAMPTZ     NOT NULL,
    PRIMARY KEY (id)
);

COMMENT ON TABLE resume_analyses IS '简历 AI 分析结果表';
COMMENT ON COLUMN resume_analyses.id IS '主键';
COMMENT ON COLUMN resume_analyses.resume_id IS '[逻辑外键]→resumes';
COMMENT ON COLUMN resume_analyses.overall_score IS 'AI 综合评分 (0-100)';
COMMENT ON COLUMN resume_analyses.strengths_json IS '优点列表 (JSON)';
COMMENT ON COLUMN resume_analyses.suggestions_json IS '改进建议 (JSON)';
COMMENT ON COLUMN resume_analyses.analyzed_at IS '评测时间';
COMMENT ON COLUMN resume_analyses.is_deleted IS '逻辑删除';
COMMENT ON COLUMN resume_analyses.trace_id IS '调用链 ID（追溯大模型响应）';
COMMENT ON COLUMN resume_analyses.created_at IS '创建时间';

CREATE INDEX IF NOT EXISTS idx_resume_analyses_resume_id ON resume_analyses (resume_id);


-- ==================== 12. candidate_skill_scores ====================
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


-- ==================== 13. candidate_profile ====================
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


-- ==================== 14. job_applications ====================
CREATE TABLE IF NOT EXISTS job_applications (
    id              BIGINT          NOT NULL,
    enterprise_id   BIGINT          NOT NULL,
    job_id          BIGINT          NOT NULL,
    candidate_id    BIGINT          NOT NULL,
    resume_id       BIGINT          NOT NULL,
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
COMMENT ON COLUMN job_applications.ai_match_score IS '大模型计算的人岗匹配度打分';
COMMENT ON COLUMN job_applications.status IS 'APPLIED / REVIEWING / PASSED / REJECTED';
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


-- ==================== 15. local_message ====================
CREATE TABLE IF NOT EXISTS local_message (
    id              BIGINT          NOT NULL,
    topic           VARCHAR(64)     NOT NULL,
    payload         JSONB           NOT NULL,
    priority        VARCHAR(16)     DEFAULT 'MEDIUM',
    status          VARCHAR(16)     DEFAULT 'PENDING',
    retry_count     INT             DEFAULT 0,
    max_retries     INT             DEFAULT 3,
    next_retry_at   TIMESTAMPTZ,
    retry_history   JSONB,
    last_error      TEXT,
    trace_id        VARCHAR(128),
    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ,
    PRIMARY KEY (id)
);

COMMENT ON TABLE local_message IS '通用本地消息表（异步补偿/重试）';
COMMENT ON COLUMN local_message.id IS '主键';
COMMENT ON COLUMN local_message.topic IS '消息类型：FILE_DELETE / SEND_SMS 等';
COMMENT ON COLUMN local_message.payload IS '业务数据 JSON';
COMMENT ON COLUMN local_message.priority IS '优先级：HIGH(5s/5次) / MEDIUM(60s/3次) / LOW(30min/1次)';
COMMENT ON COLUMN local_message.status IS 'PENDING / SUCCESS / FAILED / IGNORED';
COMMENT ON COLUMN local_message.retry_count IS '已重试次数';
COMMENT ON COLUMN local_message.max_retries IS '最大重试次数';
COMMENT ON COLUMN local_message.next_retry_at IS '下次重试时间（指数退避）';
COMMENT ON COLUMN local_message.retry_history IS '重试历史数组：[{"retry":1,"at":"...","error":"...","traceId":"..."}]';
COMMENT ON COLUMN local_message.last_error IS '最近一次失败原因';
COMMENT ON COLUMN local_message.trace_id IS '触发该消息的调用链 ID';
COMMENT ON COLUMN local_message.created_at IS '创建时间';
COMMENT ON COLUMN local_message.updated_at IS '更新时间';

CREATE INDEX IF NOT EXISTS idx_lmsg_status_retry ON local_message (status, next_retry_at);
CREATE INDEX IF NOT EXISTS idx_lmsg_priority ON local_message (priority, status);


-- ==================== 16. llm_provider_config ====================
CREATE TABLE IF NOT EXISTS llm_provider_config (
    id                  VARCHAR(64)     NOT NULL,
    base_url            VARCHAR(512)    NOT NULL,
    api_key_ciphertext  TEXT            NOT NULL,
    model               VARCHAR(128)    NOT NULL,
    enabled             BOOLEAN         NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL,
    is_deleted          BOOLEAN         DEFAULT FALSE,
    PRIMARY KEY (id)
);

COMMENT ON TABLE llm_provider_config IS '大模型路由密钥表';
COMMENT ON COLUMN llm_provider_config.id IS '提供商 ID (如 dashscope, openai)';
COMMENT ON COLUMN llm_provider_config.base_url IS 'API 网关地址';
COMMENT ON COLUMN llm_provider_config.api_key_ciphertext IS 'AES 加密存储的 API Key';
COMMENT ON COLUMN llm_provider_config.model IS '主力对话模型名';
COMMENT ON COLUMN llm_provider_config.enabled IS '路由开关';
COMMENT ON COLUMN llm_provider_config.created_at IS '创建时间';
COMMENT ON COLUMN llm_provider_config.is_deleted IS '逻辑删除';


-- ==================== 17. llm_global_setting ====================
CREATE TABLE IF NOT EXISTS llm_global_setting (
    id                          BIGINT          NOT NULL,
    default_chat_provider_id    VARCHAR(64),
    default_embedding_provider_id VARCHAR(64),
    created_at                  TIMESTAMPTZ     NOT NULL,
    updated_at                  TIMESTAMPTZ,
    PRIMARY KEY (id)
);

COMMENT ON TABLE llm_global_setting IS 'LLM 全局单例配置表';
COMMENT ON COLUMN llm_global_setting.id IS '全局单例主键，固定为 1';
COMMENT ON COLUMN llm_global_setting.default_chat_provider_id IS '[逻辑外键]→llm_provider_config, 默认对话模型提供商';
COMMENT ON COLUMN llm_global_setting.default_embedding_provider_id IS '[逻辑外键]→llm_provider_config, 默认 Embedding 模型提供商';
COMMENT ON COLUMN llm_global_setting.created_at IS '创建时间';
COMMENT ON COLUMN llm_global_setting.updated_at IS '更新时间';


-- ============================================================
-- 种子数据：角色（idempotent：先删后插）
-- role_scope: PLATFORM / ENTERPRISE / USER
-- ============================================================
DELETE FROM sys_roles WHERE id BETWEEN 1001 AND 3001;

-- PLATFORM 域（平台运营端 Admin）
INSERT INTO sys_roles (id, role_code, role_name, role_scope, created_at) VALUES
(1001, 'SUPER_ADMIN',       '超级管理员',   'PLATFORM',   NOW()),
(1002, 'FINANCE_ADMIN',     '财务管理员',   'PLATFORM',   NOW()),
(1003, 'PLATFORM_OPS',      '平台运维',     'PLATFORM',   NOW());

-- ENTERPRISE 域（B 端企业，通过 enterprise_team_members.role_id 分配）
INSERT INTO sys_roles (id, role_code, role_name, role_scope, created_at) VALUES
(2001, 'ENTERPRISE_OWNER',  '企业所有者',   'ENTERPRISE', NOW()),
(2002, 'ENTERPRISE_ADMIN',  '企业管理员',   'ENTERPRISE', NOW()),
(2003, 'HR_MANAGER',        'HR 经理',      'ENTERPRISE', NOW()),
(2004, 'HR_RECRUITER',      '招聘专员',     'ENTERPRISE', NOW()),
(2005, 'INTERVIEWER',       '面试官',       'ENTERPRISE', NOW());

-- USER 域（C 端求职者，注册时 sys_user_roles 自动分配）
INSERT INTO sys_roles (id, role_code, role_name, role_scope, created_at) VALUES
(3001, 'CANDIDATE',         '求职者',       'USER',       NOW());


-- ============================================================
-- 种子数据：sys_permissions + sys_role_permissions
-- Phase 1: 企业管理 / 团队 / 岗位 / 角色查阅 / 用户角色关联
-- Phase 2: 简历 / 投递 / 本地消息运维
-- ============================================================

-- ===== sys_permissions (Phase 1 + Phase 2 合并) =====

INSERT INTO sys_permissions (id, perm_code, perm_type, api_path, status) VALUES
-- 1.2 企业管理
(101, 'enterprise:create', 'API', '/api/v1/enterprises', 1),
(102, 'enterprise:list',   'API', '/api/v1/enterprises', 1),
(103, 'enterprise:detail', 'API', '/api/v1/enterprises/*', 1),
(104, 'enterprise:update', 'API', '/api/v1/enterprises/*', 1),
(105, 'enterprise:delete', 'API', '/api/v1/enterprises/*', 1),
(106, 'enterprise:update-contact', 'API', '/api/v1/enterprises/*/contact', 1),

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

-- 2.2 投递与初筛
(411, 'application:apply',           'API', '/api/v1/jobs/*/apply', 1),
(412, 'application:list',            'API', '/api/v1/enterprises/*/jobs/*/applications', 1),
(413, 'application:detail',          'API', '/api/v1/enterprises/*/applications/*', 1),
(414, 'application:update-status',   'API', '/api/v1/enterprises/*/applications/*/status', 1),
(415, 'candidate:applications',      'API', '/api/v1/candidate/applications', 1),

-- 2.5 本地消息管理
(421, 'ops:local-message:page',        'API', '/api/v1/admin/local-messages/page', 1),
(422, 'ops:local-message:detail',      'API', '/api/v1/admin/local-messages/*', 1),
(423, 'ops:local-message:retry',       'API', '/api/v1/admin/local-messages/*/retry', 1),
(424, 'ops:local-message:batch-retry', 'API', '/api/v1/admin/local-messages/batch-retry', 1),
(425, 'ops:local-message:status',      'API', '/api/v1/admin/local-messages/*/status', 1);


-- ===== sys_role_permissions =====

-- SUPER_ADMIN (1001) — 全部 37 个权限
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(1001, 101, NOW()), (1001, 102, NOW()), (1001, 103, NOW()), (1001, 104, NOW()), (1001, 105, NOW()), (1001, 106, NOW()),
(1001, 111, NOW()), (1001, 112, NOW()), (1001, 113, NOW()), (1001, 114, NOW()),
(1001, 203, NOW()), (1001, 204, NOW()),
(1001, 221, NOW()), (1001, 222, NOW()), (1001, 223, NOW()),
(1001, 301, NOW()), (1001, 302, NOW()), (1001, 303, NOW()), (1001, 304, NOW()), (1001, 305, NOW()), (1001, 306, NOW()),
(1001, 401, NOW()), (1001, 402, NOW()), (1001, 403, NOW()), (1001, 404, NOW()), (1001, 405, NOW()), (1001, 406, NOW()),
(1001, 411, NOW()), (1001, 412, NOW()), (1001, 413, NOW()), (1001, 414, NOW()), (1001, 415, NOW()),
(1001, 421, NOW()), (1001, 422, NOW()), (1001, 423, NOW()), (1001, 424, NOW()), (1001, 425, NOW());

-- FINANCE_ADMIN (1002) — 企业查看 + 简历查看
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(1002, 102, NOW()), (1002, 103, NOW()),
(1002, 402, NOW()), (1002, 403, NOW());

-- PLATFORM_OPS (1003) — 仅本地消息运维 5 条
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(1003, 421, NOW()), (1003, 422, NOW()), (1003, 423, NOW()), (1003, 424, NOW()), (1003, 425, NOW());

-- ENTERPRISE_OWNER (2001) — 企业/团队/岗位/简历/投递（无 Admin 和 ops）
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2001, 101, NOW()), (2001, 102, NOW()), (2001, 103, NOW()), (2001, 104, NOW()), (2001, 105, NOW()), (2001, 106, NOW()),
(2001, 111, NOW()), (2001, 112, NOW()), (2001, 113, NOW()), (2001, 114, NOW()),
(2001, 301, NOW()), (2001, 302, NOW()), (2001, 303, NOW()), (2001, 304, NOW()), (2001, 305, NOW()), (2001, 306, NOW()),
(2001, 401, NOW()), (2001, 402, NOW()), (2001, 403, NOW()), (2001, 404, NOW()), (2001, 405, NOW()), (2001, 406, NOW()),
(2001, 412, NOW()), (2001, 413, NOW()), (2001, 414, NOW());

-- ENTERPRISE_ADMIN (2002) — 与 OWNER 一致，仅去掉 enterprise:delete
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2002, 101, NOW()), (2002, 102, NOW()), (2002, 103, NOW()), (2002, 104, NOW()), (2002, 106, NOW()),
(2002, 111, NOW()), (2002, 112, NOW()), (2002, 113, NOW()), (2002, 114, NOW()),
(2002, 301, NOW()), (2002, 302, NOW()), (2002, 303, NOW()), (2002, 304, NOW()), (2002, 305, NOW()), (2002, 306, NOW()),
(2002, 401, NOW()), (2002, 402, NOW()), (2002, 403, NOW()), (2002, 404, NOW()), (2002, 405, NOW()), (2002, 406, NOW()),
(2002, 412, NOW()), (2002, 413, NOW()), (2002, 414, NOW());

-- HR_MANAGER (2003) — 团队查看/邀请/改角色 + 岗位全部 + 简历全部 + 投递管理全部
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2003, 111, NOW()), (2003, 112, NOW()), (2003, 113, NOW()),
(2003, 301, NOW()), (2003, 302, NOW()), (2003, 303, NOW()), (2003, 304, NOW()), (2003, 305, NOW()), (2003, 306, NOW()),
(2003, 401, NOW()), (2003, 402, NOW()), (2003, 403, NOW()), (2003, 404, NOW()), (2003, 405, NOW()), (2003, 406, NOW()),
(2003, 412, NOW()), (2003, 413, NOW()), (2003, 414, NOW());

-- HR_RECRUITER (2004) — 岗位发布/查看/编辑（无删除和开关）+ 简历除删除外 + 投递管理
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2004, 301, NOW()), (2004, 302, NOW()), (2004, 303, NOW()), (2004, 304, NOW()),
(2004, 401, NOW()), (2004, 402, NOW()), (2004, 403, NOW()), (2004, 405, NOW()), (2004, 406, NOW()),
(2004, 412, NOW()), (2004, 413, NOW()), (2004, 414, NOW());

-- INTERVIEWER (2005) — 仅查看岗位 + 查看简历和投递
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(2005, 302, NOW()), (2005, 303, NOW()),
(2005, 402, NOW()), (2005, 403, NOW()), (2005, 406, NOW()),
(2005, 412, NOW()), (2005, 413, NOW());

-- CANDIDATE (3001) — 投递简历 + 查看我的投递
INSERT INTO sys_role_permissions (role_id, permission_id, created_at) VALUES
(3001, 411, NOW()), (3001, 415, NOW());
