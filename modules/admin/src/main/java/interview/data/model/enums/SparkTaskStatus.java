package interview.data.model.enums;

/**
 * Spark 离线语料任务状态。
 */
public enum SparkTaskStatus {

    /**
     * 等待调度。
     */
    PENDING,

    /**
     * 执行中。
     */
    RUNNING,

    /**
     * 执行成功。
     */
    SUCCESS,

    /**
     * 执行失败。
     */
    FAILED,

    /**
     * 已取消。
     */
    CANCELLED
}
