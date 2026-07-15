-- ============================================================
-- Phase 2: 简历接收与 AI 初筛
-- 数据库: PostgreSQL 16+
-- 说明: 不使用物理外键，所有外键关系在应用层保证
--       时间字段使用 TIMESTAMPTZ，布尔使用 BOOLEAN，JSON 使用 JSONB
--       主键由应用层雪花算法生成，DDL 仅声明 BIGINT NOT NULL
-- ============================================================


-- ==================== 1. resumes ====================
CREATE TABLE IF NOT EXISTS resumes (
    id              BIGINT          NOT NULL,
    enterprise_id   BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    file_name       VARCHAR(256),
    file_size       BIGINT,
    file_type       VARCHAR(16),
    file_hash       VARCHAR(64)     NOT NULL,
    storage_url     TEXT,
    resume_text     TEXT,
    analyze_status  VARCHAR(20),
    created_at     TIMESTAMPTZ     NOT NULL,
    is_deleted      BOOLEAN         DEFAULT FALSE,
    updated_by      BIGINT,
    trace_id        VARCHAR(128),
    updated_at      TIMESTAMPTZ,
    PRIMARY KEY (id)
);

COMMENT ON TABLE resumes IS '简历底座表';
COMMENT ON COLUMN resumes.id IS '主键';
COMMENT ON COLUMN resumes.enterprise_id IS '[逻辑外键]→enterprises';
COMMENT ON COLUMN resumes.user_id IS '[逻辑外键]→sys_users, 候选人用户 ID';
COMMENT ON COLUMN resumes.file_name IS '原始文件名';
COMMENT ON COLUMN resumes.file_size IS '文件大小（字节）';
COMMENT ON COLUMN resumes.file_type IS 'pdf / doc / docx';
COMMENT ON COLUMN resumes.file_hash IS 'SHA-256 文件哈希';
COMMENT ON COLUMN resumes.storage_url IS 'RustFS / OSS 存储 URL';
COMMENT ON COLUMN resumes.resume_text IS '解析后的简历纯文本';
COMMENT ON COLUMN resumes.analyze_status IS 'PENDING / PROCESSING / COMPLETED / FAILED';
COMMENT ON COLUMN resumes.created_at IS '上传时间';
COMMENT ON COLUMN resumes.is_deleted IS '逻辑删除';
COMMENT ON COLUMN resumes.updated_by IS '[逻辑外键]→sys_users';
COMMENT ON COLUMN resumes.trace_id IS '触发解析的调用链 ID';
COMMENT ON COLUMN resumes.updated_at IS '最后更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS idx_resumes_file_hash ON resumes (enterprise_id, file_hash);
CREATE INDEX IF NOT EXISTS idx_resumes_user_id ON resumes (user_id);


-- ==================== 2. resume_analyses ====================
CREATE TABLE IF NOT EXISTS resume_analyses (
    id              BIGINT          NOT NULL,
    enterprise_id   BIGINT          NOT NULL,
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
COMMENT ON COLUMN resume_analyses.enterprise_id IS '[逻辑外键]→enterprises';
COMMENT ON COLUMN resume_analyses.resume_id IS '[逻辑外键]→resumes';
COMMENT ON COLUMN resume_analyses.overall_score IS 'AI 综合评分 (0-100)';
COMMENT ON COLUMN resume_analyses.strengths_json IS '优点列表 (JSON)';
COMMENT ON COLUMN resume_analyses.suggestions_json IS '改进建议 (JSON)';
COMMENT ON COLUMN resume_analyses.analyzed_at IS '评测时间';
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
