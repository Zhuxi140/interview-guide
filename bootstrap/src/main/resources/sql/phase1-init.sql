-- ============================================================
-- Phase 1: 地基搭建
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
    nickname        VARCHAR(64),
    avatar_url      VARCHAR(512),
    phone           VARCHAR(20),
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
COMMENT ON COLUMN sys_users.user_type IS '用户类型 (PLATFORM_ADMIN / HR / CANDIDATE)';
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
    refresh_token_hash  VARCHAR(128)    NOT NULL,
    device_info         VARCHAR(256),
    ip_address          VARCHAR(45),
    expires_at          TIMESTAMPTZ     NOT NULL,
    is_revoked          BOOLEAN         DEFAULT FALSE,
    is_deleted          BOOLEAN         DEFAULT FALSE,
    trace_id            VARCHAR(64),
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


-- ==================== 4. sys_roles ====================
CREATE TABLE IF NOT EXISTS sys_roles (
    id          BIGINT          NOT NULL,
    role_code   VARCHAR(32)     NOT NULL,
    role_name   VARCHAR(64)     NOT NULL,
    role_scope  VARCHAR(16)     NOT NULL,
    updated_by  BIGINT,
    trace_id    VARCHAR(64),
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


-- ==================== 3. sys_permissions ====================
CREATE TABLE IF NOT EXISTS sys_permissions (
    id          BIGINT          NOT NULL,
    perm_code   VARCHAR(64)     NOT NULL,
    perm_type   VARCHAR(16)     NOT NULL,
    api_path    VARCHAR(256),
    status      SMALLINT        DEFAULT 1,
    updated_by  BIGINT,
    trace_id    VARCHAR(64),
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


-- ==================== 5. sys_user_roles ====================
CREATE TABLE IF NOT EXISTS sys_user_roles (
    user_id     BIGINT          NOT NULL,
    role_id     BIGINT          NOT NULL,
    updated_by  BIGINT,
    trace_id    VARCHAR(64),
    updated_at  TIMESTAMPTZ,
    PRIMARY KEY (user_id, role_id)
);

COMMENT ON TABLE sys_user_roles IS '用户角色关联表';
COMMENT ON COLUMN sys_user_roles.user_id IS '关联用户 ID';
COMMENT ON COLUMN sys_user_roles.role_id IS '关联角色 ID';
COMMENT ON COLUMN sys_user_roles.updated_by IS '操作人 ID';
COMMENT ON COLUMN sys_user_roles.trace_id IS '调用链 ID';
COMMENT ON COLUMN sys_user_roles.updated_at IS '更新时间';


-- ==================== 6. sys_role_permissions ====================
CREATE TABLE IF NOT EXISTS sys_role_permissions (
    role_id         BIGINT      NOT NULL,
    permission_id   BIGINT      NOT NULL,
    updated_by      BIGINT,
    trace_id        VARCHAR(64),
    updated_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (role_id, permission_id)
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
    short_name      VARCHAR(64),
    industry        VARCHAR(64),
    scale           VARCHAR(32),
    contact_email   VARCHAR(128),
    contact_phone   VARCHAR(20),
    status          SMALLINT        DEFAULT 1,
    logo_url        VARCHAR(512),
    created_at      TIMESTAMPTZ     NOT NULL,
    is_deleted      BOOLEAN         DEFAULT FALSE,
    trace_id        VARCHAR(64),
    updated_at      TIMESTAMPTZ,
    PRIMARY KEY (id)
);

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
    role_id         BIGINT          NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL,
    is_deleted      BOOLEAN         DEFAULT FALSE,
    updated_by      BIGINT,
    trace_id        VARCHAR(64),
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

CREATE UNIQUE INDEX IF NOT EXISTS idx_team_member ON enterprise_team_members (enterprise_id, user_id);


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
    trace_id        VARCHAR(64),
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
