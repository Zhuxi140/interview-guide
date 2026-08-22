package interview.data.model.enums;

/**
 * 归档任务状态。
 */
public enum ArchiveTaskStatus {

    /**
     * 等待执行。
     */
    PENDING,

    /**
     * 归档执行中。
     */
    PROCESSING,

    /**
     * 归档完成。
     */
    COMPLETED,

    /**
     * 归档失败。
     */
    FAILED,

    /**
     * 已取消。
     */
    CANCELLED
}
