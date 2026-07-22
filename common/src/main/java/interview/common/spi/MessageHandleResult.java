package interview.common.spi;

import interview.common.enums.MessageHandleStatus;

import java.time.OffsetDateTime;

/**
 * 消息处理结果。
 *
 * @param status 处理状态
 * @param retryAt 下次重试时间
 * @param error 错误原因
 */
public record MessageHandleResult(
        MessageHandleStatus status,
        OffsetDateTime retryAt,
        String error
) {

    public static MessageHandleResult success() {
        return new MessageHandleResult(MessageHandleStatus.SUCCESS, null, null);
    }

    public static MessageHandleResult ignored(String reason) {
        return new MessageHandleResult(MessageHandleStatus.IGNORED, null, reason);
    }

    public static MessageHandleResult retry(OffsetDateTime retryAt, String reason) {
        return new MessageHandleResult(MessageHandleStatus.RETRY, retryAt, reason);
    }

    public static MessageHandleResult permanentFailure(String reason) {
        return new MessageHandleResult(MessageHandleStatus.PERMANENT_FAILURE, null, reason);
    }
}
