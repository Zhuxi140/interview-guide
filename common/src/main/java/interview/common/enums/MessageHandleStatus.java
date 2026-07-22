package interview.common.enums;

/**
 * 消息处理结果状态。
 */
public enum MessageHandleStatus {
    SUCCESS,
    IGNORED,
    RETRY,
    PERMANENT_FAILURE
}
