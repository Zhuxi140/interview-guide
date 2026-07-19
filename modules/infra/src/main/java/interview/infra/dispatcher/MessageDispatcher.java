package interview.infra.dispatcher;

import interview.common.constant.Message;
import interview.common.enums.MsgPriority;
import interview.common.enums.MsgStatus;
import interview.common.enums.MsgTopic;
import interview.common.spi.MessageHandler;
import interview.infra.localMessage.model.entity.LocalMessage;
import interview.infra.localMessage.service.LocalMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author zhuxi
 */
@Slf4j
@Component
public class MessageDispatcher {

    private final Map<MsgTopic, MessageHandler> handlerMap;
    private final LocalMessageService localMessageService;

    public MessageDispatcher(List<MessageHandler> handlers, LocalMessageService localMessageService) {
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(MessageHandler::getTopic, Function.identity()));
        this.localMessageService = localMessageService;
    }

    @Scheduled(fixedDelay = 5000)
    public void processHigh() {
        dispatch(MsgPriority.HIGH);
    }

    @Scheduled(fixedDelay = 60000)
    public void processMedium() {
        dispatch(MsgPriority.MEDIUM);
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void processLow() {
        dispatch(MsgPriority.LOW);
    }

    public void dispatch(MsgPriority priority) {
        List<LocalMessage> messages = localMessageService.fetchAndLockForDispatch(priority, 100);
        if (messages.isEmpty()) {
            return;
        }

        for (LocalMessage msg : messages) {
            MessageHandler handler = handlerMap.get(msg.getTopic());
            if (handler == null) {
                log.warn("未找到 topic 处理器: {}, 标记为 FAILED", msg.getTopic());
                localMessageService.markFailed(msg.getId(), 0, MsgStatus.FAILED, OffsetDateTime.now(), "no handler for topic: " + msg.getTopic());
                continue;
            }
            try {
                handler.handle(new Message(msg.getTopic(), msg.getPayload()));
                localMessageService.markSuccess(msg.getId());
            } catch (Exception e) {
                int retry = (msg.getRetryCount() != null ? msg.getRetryCount() : 0) + 1;
                int maxRetries = msg.getMaxRetries() != null ? msg.getMaxRetries() : (priority == MsgPriority.LOW ? 2 : 3);
                boolean reachedLimit = retry >= maxRetries;
                MsgStatus newStatus = reachedLimit ? MsgStatus.FAILED : MsgStatus.PENDING;
                long delay = calculateBackoff(priority, retry);
                OffsetDateTime nextRetry = OffsetDateTime.now().plusSeconds(delay);

                log.warn("消息处理失败, id={}, topic={}, retry={}/{}, next={}",
                        msg.getId(), msg.getTopic(), retry, maxRetries, nextRetry, e);

                localMessageService.markFailed(msg.getId(), retry, newStatus, nextRetry, e.getMessage());
                // TODO: 监控告警 — 扫描 FAILED 数量，接入通知服务
            }
        }
    }

    private long calculateBackoff(MsgPriority priority, int retryCount) {
        long base = switch (priority) {
            case HIGH -> 10;
            case MEDIUM -> 30;
            case LOW -> 60;
        };
        return base * (long) Math.pow(2, retryCount - 1);
    }
}
