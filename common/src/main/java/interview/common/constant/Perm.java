package interview.common.constant;

/**
 * @author zhuxi
 */
public interface Perm {

    interface Enterprise {
        String CREATE = "enterprise:create";
        String LIST = "enterprise:list";
        String DETAIL = "enterprise:detail";
        String UPDATE = "enterprise:update";
        String DELETE = "enterprise:delete";
        String UPDATE_CONTACT = "enterprise:update-contact";
    }

    interface Team {
        String LIST = "team:list";
        String INVITE = "team:invite";
        String UPDATE = "team:update";
        String REMOVE = "team:remove";
    }

    interface AdminRoles {
        String LIST = "admin:roles:list";
        String DETAIL = "admin:roles:detail";
    }

    interface AdminUserRoles {
        String ASSIGN = "admin:user-roles:assign";
        String LIST = "admin:user-roles:list";
        String REMOVE = "admin:user-roles:remove";
    }

    interface AdminUsers {
        String LIST = "admin:users:list";
        String DETAIL = "admin:users:detail";
    }

    interface AdminCodeQuestion {
        String CREATE = "code-question:create";
        String LIST = "code-question:list";
        String DETAIL = "code-question:detail";
        String UPDATE = "code-question:update";
        String DELETE = "code-question:delete";
    }

    interface CodeTestCase {
        String CREATE = "code-test-case:create";
        String UPDATE = "code-test-case:update";
        String DELETE = "code-test-case:delete";
    }

    interface CodeSubmission {
        String SUBMIT = "code-submission:submit";
        String RESULTS = "code-submission:results";
    }

    interface KnowledgeBase {
        String CREATE = "knowledge-base:create";
        String LIST = "knowledge-base:list";
        String DETAIL = "knowledge-base:detail";
        String DELETE = "knowledge-base:delete";
        String VECTORIZE = "knowledge-base:vectorize";
    }

    interface Rag {
        String SESSION_CREATE = "rag:session-create";
        String SESSION_LIST = "rag:session-list";
        String MESSAGE_SEND = "rag:message-send";
        String MESSAGE_LIST = "rag:message-list";
        String KNOWLEDGE_BIND = "rag:session-knowledge-bind";
    }

    interface Kyc {
        String SUBMIT = "kyc:submit";
        String STATUS = "kyc:status";
        String ADMIN_AUDIT = "admin:kyc:audit";
        String ADMIN_LIST = "admin:kyc:list";
    }

    interface EnterpriseCert {
        String SUBMIT = "enterprise:cert:submit";
        String STATUS = "enterprise:cert:status";
        String ADMIN_AUDIT = "admin:cert:audit";
        String ADMIN_LIST = "admin:cert:list";
    }

    interface SensitiveWords {
        String LIST = "admin:sensitive-words:list";
        String CREATE = "admin:sensitive-words:create";
        String UPDATE = "admin:sensitive-words:update";
        String DELETE = "admin:sensitive-words:delete";
    }

    interface AntiCheat {
        String ADMIN_LIST = "admin:anti-cheat:list";
        String ADMIN_DETAIL = "admin:anti-cheat:detail";
    }

    interface ApiPolicies {
        String CREATE = "admin:api-policies:create";
        String LIST = "admin:api-policies:list";
        String UPDATE = "admin:api-policies:update";
        String DELETE = "admin:api-policies:delete";
    }

    interface CalendarSlots {
        String CREATE = "enterprise:calendar-slots:create";
        String LIST = "enterprise:calendar-slots:list";
        String DELETE = "enterprise:calendar-slots:delete";
    }

    interface Notification {
        String LIST = "notification:list";
        String READ = "notification:read";
        String READ_ALL = "notification:read-all";
    }

    interface NotificationAdmin {
        String CHANNEL_VIEW = "admin:notification-channels:view";
        String CHANNEL_UPDATE = "admin:notification-channels:update";
        String TEMPLATE_LIST = "admin:notification-templates:list";
        String TEMPLATE_CREATE = "admin:notification-templates:create";
        String TEMPLATE_UPDATE = "admin:notification-templates:update";
        String SEND_LIST = "admin:notifications:list";
    }

    interface Tutor {
        String CREATE_SESSION = "candidate:tutor:create-session";
        String LIST_SESSIONS = "candidate:tutor:list-sessions";
        String SEND_MESSAGE = "candidate:tutor:send-message";
        String LIST_MESSAGES = "candidate:tutor:list-messages";
    }

    interface SparkTasks {
        String LIST = "admin:spark-tasks:list";
        String DETAIL = "admin:spark-tasks:detail";
    }

    interface Audit {
        String API_LOGS_LIST = "admin:audit:api-logs:list";
        String API_LOGS_DETAIL = "admin:audit:api-logs:detail";
        String API_LOGS_ARCHIVE = "admin:audit:api-logs:archive";
        String OPERATE_LOGS_LIST = "admin:audit:operate-logs:list";
        String OPERATE_LOGS_DETAIL = "admin:audit:operate-logs:detail";
        String OPERATE_LOGS_TRACE = "admin:audit:operate-logs:trace";
        String ENTERPRISE_OPERATE_LOGS_LIST = "enterprise:audit:operate-logs:list";
        String ENTERPRISE_OPERATE_LOGS_DETAIL = "enterprise:audit:operate-logs:detail";
        String DATA_RETENTION_VIEW = "admin:data-retention:view";
        String DATA_RETENTION_UPDATE = "admin:data-retention:update";
        String ARCHIVE_TASKS_CREATE = "admin:archive-tasks:create";
        String ARCHIVE_TASKS_LIST = "admin:archive-tasks:list";
        String ARCHIVE_TASKS_DETAIL = "admin:archive-tasks:detail";
    }

    interface Job {
        String CREATE = "job:create";
        String LIST = "job:list";
        String DETAIL = "job:detail";
        String UPDATE = "job:update";
        String TOGGLE_STATUS = "job:toggle-status";
        String DELETE = "job:delete";
    }

    interface Resume {
        String UPLOAD = "resume:upload";
        String LIST = "resume:list";
        String DETAIL = "resume:detail";
        String DELETE = "resume:delete";
        String ANALYZE = "resume:analyze";
        String ANALYSIS_RESULT = "resume:analysis-result";
        String DOWNLOAD = "resume:download";
    }

    interface Application {
        String APPLY = "application:apply";
        String LIST = "application:list";
        String DETAIL = "application:detail";
        String UPDATE = "application:update-status";
        String MY_LIST = "candidate:applications";
        String WITHDRAW = "candidate:application:withdraw";
        String SCREENING_CONFIG_DETAIL = "application:screening-config:detail";
        String SCREENING_CONFIG_UPDATE = "application:screening-config:update";
        String AI_SCREENING_CREATE = "application:ai-screening:create";
        String AI_SCREENING_DETAIL = "application:ai-screening:detail";
        String AI_SCREENING_REVIEW = "application:ai-screening:review";
        String CANDIDATE_PROFILE_DETAIL = "application:candidate-profile:detail";
        String MATCH_ANALYSIS_CREATE = "candidate:application:match-analysis:create";
        String MATCH_ANALYSIS_DETAIL = "candidate:application:match-analysis:detail";
    }

    interface ResumeImport {
        String DETAIL = "resume:import-batch:detail";
        String ITEMS = "resume:import-batch:items";
    }

    interface EnterpriseCandidate {
        String LIST = "enterprise:candidates:list";
        String DETAIL = "enterprise:candidates:detail";
        String OVERVIEW = "enterprise:candidates:overview";
    }

    interface AdminLlm {
        String PROVIDER_CREATE = "admin:llm:provider:create";
        String PROVIDER_LIST = "admin:llm:provider:list";
        String PROVIDER_DETAIL = "admin:llm:provider:detail";
        String PROVIDER_UPDATE = "admin:llm:provider:update";
        String PROVIDER_STATUS = "admin:llm:provider:status";
        String PROVIDER_DELETE = "admin:llm:provider:delete";
        String PROVIDER_TEST = "admin:llm:provider:test";
        String SCENE_LIST = "admin:llm:scene:list";
        String SCENE_DETAIL = "admin:llm:scene:detail";
        String SCENE_UPDATE = "admin:llm:scene:update";
        String SCENE_STATUS = "admin:llm:scene:status";
    }

    interface AdminAi {
        String ROUTE_LIST = "admin:ai:route:list";
        String ROUTE_UPDATE = "admin:ai:route:update";
    }

    interface InterviewTemplate {
        String LIST = "interview-template:list";
        String DETAIL = "interview-template:detail";
        String PHASE_CONFIG_LIST = "interview-template:phase-configs:list";
    }

    interface InterviewSchedule {
        String CREATE = "interview-schedule:create";
        String AI_SUGGEST = "interview-schedule:ai-suggest";
        String LIST = "interview-schedule:list";
        String DETAIL = "interview-schedule:detail";
    }

    interface InterviewReport {
        String DETAIL = "interview-report:detail";
        String LIST = "enterprise:interview-reports";
    }

    interface ApplicationTransitionLog {
        String LIST = "enterprise:application-transition-logs";
    }

    interface AdminBilling {
        String SKU_LIST = "admin:billing:sku:list";
        String SKU_DETAIL = "admin:billing:sku:detail";
        String SKU_CREATE = "admin:billing:sku:create";
        String SKU_UPDATE = "admin:billing:sku:update";
        String SKU_STATUS = "admin:billing:sku:status";
        String ORDER_LIST = "admin:billing:order:list";
        String ORDER_DETAIL = "admin:billing:order:detail";
        String ORDER_SIMULATE_PAYMENT = "admin:billing:order:simulate-payment";
    }

    interface BillingOrder {
        String CREATE = "billing:order:create";
        String PAY = "billing:order:pay";
        String LIST = "billing:order:list";
        String DETAIL = "billing:order:detail";
        String CANCEL = "billing:order:cancel";
    }

    interface BillingWallet {
        String DETAIL = "billing:wallet:detail";
        String TRANSACTIONS = "billing:wallet:transactions";
    }

    interface BillingConsumeLog {
        String LIST = "billing:consume-log:list";
    }

    interface Ops {
        String LOCAL_MESSAGE_PAGE = "ops:local-message:page";
        String LOCAL_MESSAGE_DETAIL = "ops:local-message:detail";
        String LOCAL_MESSAGE_RETRY = "ops:local-message:retry";
        String LOCAL_MESSAGE_BATCH_RETRY = "ops:local-message:batch-retry";
        String LOCAL_MESSAGE_STATUS = "ops:local-message:status";
    }
}
