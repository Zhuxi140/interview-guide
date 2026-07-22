package interview.infra.dispatcher;

import interview.common.constant.MessageEnvelope;
import interview.common.enums.MessageHandleStatus;
import interview.common.enums.MsgPriority;
import interview.common.enums.MsgStatus;
import interview.common.spi.MessageHandleResult;
import interview.common.spi.MessageTransport;
import interview.infra.localMessage.model.entity.LocalMessage;
import interview.infra.localMessage.service.LocalMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 使用数据库租约派发本地 Outbox 消息。
 */
@Slf4j
@Component
public class MessageDispatcher {

    private static final long LEASE_SECONDS = 120;
    private static final int BATCH_SIZE = 100;
    private static final int DEFAULT_MAX_RETRIES = 5;
    private static final long[] RETRY_MINUTES = {1, 5, 15, 30, 60};

    private final String workerId = "local-" + UUID.randomUUID();
    private final MessageTransport messageTransport;
    private final LocalMessageService localMessageService;

    public MessageDispatcher(MessageTransport messageTransport, LocalMessageService localMessageService) {
        this.messageTransport = messageTransport;
        this.localMessageService = localMessageService;
    }

    @Scheduled(fixedDelayString = "${app.message.dispatch.high-delay-ms:5000}")
    public void processHigh() {
        dispatch(MsgPriority.HIGH);
    }

    @Scheduled(fixedDelayString = "${app.message.dispatch.medium-delay-ms:60000}")
    public void processMedium() {
        dispatch(MsgPriority.MEDIUM);
    }

    @Scheduled(fixedDelayString = "${app.message.dispatch.low-delay-ms:60000}")
    public void processLow() {
        dispatch(MsgPriority.LOW);
    }

    public void dispatch(MsgPriority priority) {
        // 领取事务已结束，业务处理不会长期持有 local_message 行锁。
        List<LocalMessage> messages = localMessageService.claimForDispatch(
                priority, workerId, LEASE_SECONDS, BATCH_SIZE
        );
        for (LocalMessage message : messages) {
            dispatchOne(message);
        }
    }

    private void dispatchOne(LocalMessage message) {
        try {
            MessageHandleResult result = messageTransport.dispatch(toEnvelope(message));
            applyResult(message, result);
        } catch (Exception e) {
            // 未分类异常按临时故障进行指数退避。
            scheduleRetry(message, null, e.getMessage());
            log.warn("消息处理失败, id={}, topic={}", message.getId(), message.getTopic(), e);
        }
    }

    private void applyResult(LocalMessage message, MessageHandleResult result) {
        boolean updated;
        if (result.status() == MessageHandleStatus.SUCCESS) {
            updated = localMessageService.markSuccessIfOwned(message);
        } else if (result.status() == MessageHandleStatus.IGNORED) {
            updated = localMessageService.markIgnoredIfOwned(message, result.error());
        } else if (result.status() == MessageHandleStatus.PERMANENT_FAILURE) {
            updated = localMessageService.markRetryIfOwned(
                    message, currentRetryCount(message), MsgStatus.FAILED, null, result.error()
            );
        } else {
            updated = scheduleRetry(message, result.retryAt(), result.error());
        }

        if (!updated) {
            log.info("消息租约已经失效，忽略旧执行者结果。id={}, leaseVersion={}",
                    message.getId(), message.getLeaseVersion());
        }
    }

    private boolean scheduleRetry(LocalMessage message, OffsetDateTime requestedRetryAt, String error) {
        int retryCount = currentRetryCount(message) + 1;
        int maxRetries = message.getMaxRetries() == null
                ? DEFAULT_MAX_RETRIES : message.getMaxRetries();
        // maxRetries 表示首次执行失败后允许再次执行的次数。
        boolean reachedLimit = retryCount > maxRetries;
        MsgStatus status = reachedLimit ? MsgStatus.FAILED : MsgStatus.PENDING;
        OffsetDateTime nextRetry = reachedLimit ? null
                : requestedRetryAt != null ? requestedRetryAt : calculateRetryAt(retryCount);
        return localMessageService.markRetryIfOwned(
                message, retryCount, status, nextRetry, error
        );
    }

    private int currentRetryCount(LocalMessage message) {
        return message.getRetryCount() == null ? 0 : message.getRetryCount();
    }

    private OffsetDateTime calculateRetryAt(int retryCount) {
        int index = Math.min(Math.max(retryCount - 1, 0), RETRY_MINUTES.length - 1);
        return OffsetDateTime.now().plusMinutes(RETRY_MINUTES[index]);
    }

    private MessageEnvelope toEnvelope(LocalMessage message) {
        return MessageEnvelope.builder()
                .messageId(message.getId())
                .topic(message.getTopic())
                .schemaVersion(message.getSchemaVersion())
                .payload(message.getPayload())
                .traceId(message.getTraceId())
                .build();
    }
}
