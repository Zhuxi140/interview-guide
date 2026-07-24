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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;

/**
 * 使用数据库租约派发本地 Outbox 消息。
 */
@Slf4j
@Component
public class MessageDispatcher {

    private static final int DEFAULT_MAX_RETRIES = 5;
    private static final long[] RETRY_MINUTES = {1, 5, 15, 30, 60};
    private final String workerId = "local-" + UUID.randomUUID();
    private final MessageTransport messageTransport;
    private final LocalMessageService localMessageService;
    private final Executor messageHandlerExecutor;
    private final MessageDispatchProperties properties;
    private final Semaphore executionSlots;

    public MessageDispatcher(MessageTransport messageTransport,
                             LocalMessageService localMessageService,
                             @Qualifier("messageHandlerExecutor") Executor messageHandlerExecutor,
                             MessageDispatchProperties properties) {
        this.messageTransport = messageTransport;
        this.localMessageService = localMessageService;
        this.messageHandlerExecutor = messageHandlerExecutor;
        this.properties = properties;
        this.executionSlots = new Semaphore(properties.executionCapacity(), true);
    }

    @Scheduled(fixedDelayString = "${app.message.dispatch.high-delay-ms:5000}")
    public void processHigh() {
        dispatch(MsgPriority.HIGH);
    }

    @Scheduled(fixedDelayString = "${app.message.dispatch.medium-delay-ms:30000}")
    public void processMedium() {
        dispatch(MsgPriority.MEDIUM);
    }

    @Scheduled(fixedDelayString = "${app.message.dispatch.low-delay-ms:300000}")
    public void processLow() {
        dispatch(MsgPriority.LOW);
    }

    public void dispatch(MsgPriority priority) {
        // 先预留执行槽位，只领取线程池能够接收的消息，避免租约在内存队列中提前过期。
        int reservedSlots = reserveExecutionSlots(properties.getBatchSize());
        if (reservedSlots == 0) {
            log.debug("消息执行器已满，本轮跳过领取。priority={}", priority);
            return;
        }

        List<LocalMessage> messages = claimMessages(priority, reservedSlots);
        executionSlots.release(reservedSlots - messages.size());
        for (LocalMessage message : messages) {
            submit(message);
        }
    }

    private int reserveExecutionSlots(int maxSlots) {
        int reserved = 0;
        while (reserved < maxSlots && executionSlots.tryAcquire()) {
            reserved++;
        }
        return reserved;
    }

    private List<LocalMessage> claimMessages(MsgPriority priority, int limit) {
        try {
            // 领取事务结束后只持有租约，调度线程不再直接执行业务 Handler。
            return localMessageService.claimForDispatch(
                    priority, workerId, properties.getLeaseSeconds(), limit
            );
        } catch (RuntimeException e) {
            log.error("领取本地消息失败, priority={}", priority, e);
            return Collections.emptyList();
        }
    }

    private void submit(LocalMessage message) {
        try {
            messageHandlerExecutor.execute(() -> {
                try {
                    dispatchOne(message);
                } finally {
                    executionSlots.release();
                }
            });
        } catch (RuntimeException e) {
            // 执行器关闭或拒绝任务时释放租约，不消耗业务重试次数。
            executionSlots.release();
            releaseRejectedMessage(message, e);
        }
    }

    private void releaseRejectedMessage(LocalMessage message, RuntimeException cause) {
        String error = "message executor rejected: " + cause.getMessage();
        try {
            boolean updated = localMessageService.markRetryIfOwned(
                    message,
                    currentRetryCount(message),
                    MsgStatus.PENDING,
                    OffsetDateTime.now(),
                    error
            );
            if (!updated) {
                log.info("消息执行器拒绝任务后租约已失效。id={}, leaseVersion={}",
                        message.getId(), message.getLeaseVersion());
            }
        } catch (Exception updateException) {
            // 状态恢复失败时保留 PROCESSING，租约到期后可由其他实例重新领取。
            log.error("消息执行器拒绝任务且释放租约失败。id={}", message.getId(), updateException);
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
