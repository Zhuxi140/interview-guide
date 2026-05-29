package interview.common.enums;

/**
 * @author zhuxi
 * @since 2026-05-26
 * @apiNote 错误码枚举类
 */
public enum ErrorCode {

    // ------------ 10xxx 系统/全局级错误码 ------------
    SUCCESS(10000, "成功"),
    SYSTEM_ERROR(500, "系统错误,请稍后重试或联系客服"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    PARAM_VALID_ERROR(10001, "参数校验失败"),
    INTERFACE_NOT_FOUND(404, "接口不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    REQUEST_BODY_OVERFLOW(413, "请求实体过大"),
    NOT_SUPPORTED_MEDIA_TYPE(415, "不支持的媒体类型"),
    REQUEST_TIMEOUT(408, "请求超时"),
    DATA_TYPE_ERROR(10002, "数据类型错误"),
    OBJECT_TO_JSON_ERROR(10003, "JSON 序列化错误"),
    JSON_TO_OBJECT_ERROR(10004, "JSON 反序列化错误"),
    TRACE_ID_MISSING(10005, "调用链 ID 缺失"),
    ENCRYPTION_ERROR(10006, "加密失败"),
    DECRYPTION_ERROR(10007, "解密失败"),
    FILE_UPLOAD_FAILED(10008, "文件上传失败"),
    FILE_DOWNLOAD_FAILED(10009, "文件下载失败"),

    // ------------ 20xxx 用户认证与授权错误码 ------------
    USER_NOT_LOGIN(401, "未登录"),
    TOKEN_EXPIRED(401, "Token 已过期"),
    TOKEN_INVALID(401, "Token 无效"),
    PERMISSION_DENIED(403, "权限不足"),
    USER_ALREADY_FREEZE(403, "用户已冻结"),
    RISK_CONTROL(403, "你的账户当前存在异常，请等待稍后重试或联系客服"),
    CODE_ERROR(20001, "验证码错误"),
    CODE_EXPIRED(20002, "验证码已过期"),
    PHONE_ALREADY_EXISTS(20003, "手机号已注册"),
    THIRD_PARTY_AUTH_ERROR(20004, "第三方授权失败"),
    PASSWORD_STRENGTH_ERROR(20005, "密码强度不够"),
    USERNAME_ALREADY_EXISTS(20006, "用户名已存在"),
    EMAIL_ALREADY_EXISTS(20007, "邮箱已注册"),
    REFRESH_TOKEN_EXPIRED(20008, "Refresh Token 已过期，请重新登录"),
    REFRESH_TOKEN_REVOKED(20009, "Refresh Token 已被撤销"),
    DEVICE_LIMIT_EXCEEDED(20010, "登录设备数量超过限制"),
    USER_NOT_FOUND(20011, "用户不存在"),
    PASSWORD_ERROR(20012, "密码错误"),
    ACCOUNT_DISABLED(20013, "账号已被禁用"),
    CODE_ONE_MINUTE(20014, "验证码已经发送，请 1 分钟后再试"),

    // ------------ 30xxx 企业/多租户错误码 ------------
    ENTERPRISE_NOT_FOUND(30001, "企业不存在"),
    ENTERPRISE_FROZEN(30002, "企业已被冻结"),
    ENTERPRISE_NOT_CERTIFIED(30003, "企业未通过资质认证"),
    ENTERPRISE_MEMBER_LIMIT(30004, "企业成员数量已达上限"),
    ENTERPRISE_MEMBER_NOT_FOUND(30005, "企业成员不存在"),
    ENTERPRISE_ALREADY_EXISTS(30006, "企业已存在"),
    NOT_ENTERPRISE_OWNER(30007, "非企业所有者，无权操作"),
    ENTERPRISE_MEMBER_ALREADY_EXISTS(30008, "该用户已是企业成员"),
    ENTERPRISE_TENANT_MISMATCH(30009, "租户隔离校验不通过"),

    // ------------ 40xxx 岗位/简历/候选人错误码 ------------
    JOB_NOT_FOUND(40001, "岗位不存在"),
    JOB_ALREADY_CLOSED(40002, "岗位已关闭"),
    JOB_NOT_BELONG_TO_ENTERPRISE(40003, "岗位不属于当前企业"),
    RESUME_NOT_FOUND(40004, "简历不存在"),
    RESUME_ANALYSIS_FAILED(40005, "简历 AI 解析失败"),
    RESUME_FILE_TYPE_NOT_SUPPORTED(40006, "不支持的简历文件格式"),
    RESUME_FILE_TOO_LARGE(40007, "简历文件过大"),
    RESUME_DUPLICATE_HASH(40008, "重复的简历文件"),
    CANDIDATE_NOT_FOUND(40009, "候选人不存在"),
    JOB_APPLICATION_ALREADY_EXISTS(40010, "已投递该岗位，请勿重复投递"),
    JOB_APPLICATION_STATUS_INVALID(40011, "投递记录状态不合法"),
    RESUME_ANALYSIS_NOT_FOUND(40012, "简历分析结果不存在"),
    CANDIDATE_PROFILE_NOT_FOUND(40013, "候选人画像不存在"),

    // ------------ 50xxx 文本/面试流程错误码 ------------
    INTERVIEW_SCHEDULE_NOT_FOUND(50001, "面试排期不存在"),
    INTERVIEW_SCHEDULE_STATUS_INVALID(50002, "面试排期状态不合法"),
    INTERVIEW_SESSION_NOT_FOUND(50003, "面试会话不存在"),
    INTERVIEW_SESSION_ALREADY_COMPLETED(50004, "面试会话已结束"),
    INTERVIEW_ANSWER_NOT_FOUND(50005, "面试作答记录不存在"),
    INTERVIEW_TEMPLATE_NOT_FOUND(50006, "面试阶段模板不存在"),
    INTERVIEW_PHASE_CONFIG_NOT_FOUND(50007, "面试阶段配置不存在"),
    INTERVIEW_REPORT_NOT_FOUND(50008, "面试报告不存在"),
    INTERVIEW_TIME_CONFLICT(50009, "面试时间冲突"),
    INTERVIEW_ALREADY_IN_PROGRESS(50010, "面试正在进行中"),
    INTERVIEW_FLOW_NOT_ALLOWED(50011, "当前状态不允许该流转操作"),
    INTERVIEW_WORKFLOW_LOG_NOT_FOUND(50012, "流转日志不存在"),

    // ------------ 60xxx 计费/钱包错误码 ------------
    SKU_NOT_FOUND(60001, "套餐 SKU 不存在"),
    SKU_NOT_ACTIVE(60002, "套餐已下架"),
    WALLET_NOT_FOUND(60003, "钱包不存在"),
    INSUFFICIENT_BALANCE(60004, "算力余额不足"),
    INSUFFICIENT_FROZEN_BALANCE(60005, "冻结余额不足"),
    PAYMENT_ORDER_NOT_FOUND(60006, "订单不存在"),
    PAYMENT_ORDER_EXPIRED(60007, "订单已过期"),
    PAYMENT_ORDER_ALREADY_PAID(60008, "订单已支付"),
    TOKEN_CONSUME_LOG_FAILED(60009, "算力消耗记录失败"),
    WALLET_VERSION_CONFLICT(60010, "钱包并发更新冲突，请重试"),
    BILLING_DISABLED(60011, "计费系统暂未启用"),
    WALLET_TRANSACTION_FAILED(60012, "钱包流水记录失败"),

    // ------------ 70xxx 语音面试错误码 ------------
    VOICE_SESSION_NOT_FOUND(70001, "语音会话不存在"),
    VOICE_SESSION_ALREADY_COMPLETED(70002, "语音会话已结束"),
    VOICE_MESSAGE_NOT_FOUND(70003, "语音消息不存在"),
    VOICE_EVALUATION_NOT_FOUND(70004, "语音评估结果不存在"),
    ASR_RECOGNITION_FAILED(70005, "语音识别失败"),
    TTS_SYNTHESIS_FAILED(70006, "语音合成失败"),
    VOICE_SESSION_STATUS_INVALID(70007, "语音会话状态不合法"),

    // ------------ 80xxx 代码沙箱错误码 ------------
    CODE_QUESTION_NOT_FOUND(80001, "编程题目不存在"),
    CODE_TEST_CASE_NOT_FOUND(80002, "测试用例不存在"),
    CODE_SUBMISSION_NOT_FOUND(80003, "代码提交记录不存在"),
    CODE_SUBMISSION_RESULT_NOT_FOUND(80004, "代码执行结果不存在"),
    CODE_EXECUTION_TIMEOUT(80005, "代码执行超时"),
    CODE_EXECUTION_MEMORY_LIMIT(80006, "代码执行内存超限"),
    CODE_EXECUTION_ERROR(80007, "代码执行异常"),
    CODE_LANGUAGE_NOT_SUPPORTED(80008, "不支持的编程语言"),
    CODE_SANDBOX_UNAVAILABLE(80009, "代码沙箱服务不可用"),
    CODE_REVIEW_FAILED(80010, "AI 代码审查失败"),

    // ------------ 90xxx 知识库/RAG 错误码 ------------
    KNOWLEDGE_BASE_NOT_FOUND(90001, "知识库文档不存在"),
    KNOWLEDGE_BASE_VECTORIZE_FAILED(90002, "知识库向量化失败"),
    DOCUMENT_CHUNK_NOT_FOUND(90003, "文档切片不存在"),
    RAG_SESSION_NOT_FOUND(90004, "RAG 会话不存在"),
    RAG_MESSAGE_NOT_FOUND(90005, "RAG 消息不存在"),
    KNOWLEDGE_BASE_FILE_TOO_LARGE(90006, "知识库文件过大"),
    KNOWLEDGE_BASE_DUPLICATE_HASH(90007, "知识库重复文件"),
    KNOWLEDGE_BASE_FILE_TYPE_NOT_SUPPORTED(90008, "不支持的文档格式"),

    // ------------ 100xxx 风控/合规/KYC 错误码 ------------
    KYC_NOT_FOUND(100001, "实名认证记录不存在"),
    KYC_NOT_PASSED(100002, "实名认证未通过"),
    KYC_ALREADY_SUBMITTED(100003, "已提交实名认证，请勿重复提交"),
    ENTERPRISE_CERT_NOT_FOUND(100004, "企业认证记录不存在"),
    ENTERPRISE_CERT_NOT_PASSED(100005, "企业资质认证未通过"),
    SENSITIVE_WORD_DETECTED(100006, "内容包含敏感词"),
    ANTI_CHEAT_EVENT_DETECTED(100007, "检测到异常行为"),
    USER_API_POLICY_BLOCKED(100008, "您的访问已被策略拦截"),
    RATE_LIMIT_EXCEEDED(100009, "请求频率超过限制"),
    USER_IN_BLACKLIST(100010, "您已被加入黑名单"),

    // ------------ 110xxx 消息通知/日志错误码 ------------
    NOTIFICATION_SEND_FAILED(110001, "消息通知发送失败"),
    NOTIFICATION_NOT_FOUND(110002, "通知消息不存在"),
    API_LOG_ARCHIVE_FAILED(110003, "日志归档失败"),

    // ------------ 120xxx AI/大模型错误码 ------------
    AI_PROVIDER_NOT_FOUND(120001, "AI 供应商不存在"),
    AI_PROVIDER_DISABLED(120002, "AI 供应商已被禁用"),
    AI_API_KEY_INVALID(120003, "AI API Key 已失效"),
    AI_MODEL_NOT_FOUND(120004, "AI 模型不存在"),
    AI_RESPONSE_PARSE_ERROR(120005, "AI 响应解析失败"),
    AI_SERVICE_TIMEOUT(120006, "AI 服务响应超时"),
    AI_SERVICE_UNAVAILABLE(120007, "AI 服务暂不可用"),
    AI_EMBEDDING_FAILED(120008, "向量化失败"),
    AI_GLOBAL_SETTING_NOT_FOUND(120009, "AI 全局配置不存在"),
    AI_CONTENT_FILTERED(120010, "AI 输出内容被安全策略过滤"),
    AI_TOKEN_LIMIT_EXCEEDED(120011, "AI 上下文长度超过限制"),
    AI_SAFETY_GUARDRAIL_TRIGGERED(120012, "AI 安全护栏触发"),

    // ------------ 13xxx Spark 离线任务错误码 ------------
    SPARK_TASK_NOT_FOUND(130001, "Spark 任务不存在"),
    SPARK_TASK_STATUS_INVALID(130002, "Spark 任务状态不合法"),
    SPARK_TASK_EXECUTION_FAILED(130003, "Spark 任务执行失败"),
    ;

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
