package interview.infra.localMessage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.common.enums.ErrorCode;
import interview.common.enums.MsgPriority;
import interview.common.enums.MsgStatus;
import interview.common.exception.BusinessException;
import interview.infra.localMessage.mapper.LocalMessageMapper;
import interview.infra.localMessage.model.entity.LocalMessage;
import interview.infra.localMessage.service.LocalMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zhuxi
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocalMessageServiceImpl extends ServiceImpl<LocalMessageMapper, LocalMessage> implements LocalMessageService {

    @Override
    public IPage<LocalMessage> pageQuery(Integer page, Integer size, String status, String topic, String priority,
                                         OffsetDateTime startTime, OffsetDateTime endTime) {
        Page<LocalMessage> pageParam = new Page<>(page, size);
        QueryWrapper<LocalMessage> qw = new QueryWrapper<>();
        if (status != null) {
            qw.eq("status", status);
        }
        if (topic != null) {
            qw.eq("topic", topic);
        }
        if (priority != null) {
            qw.eq("priority", priority);
        }
        if (startTime != null) {
            qw.ge("created_at", startTime);
        }
        if (endTime != null) {
            qw.le("created_at", endTime);
        }
        qw.orderByDesc("created_at");
        return page(pageParam, qw);
    }

    @Override
    public LocalMessage getDetail(Long id) {
        LocalMessage msg = getById(id);
        if (msg == null) {
            throw new BusinessException(ErrorCode.LOCAL_MESSAGE_NOT_FOUND);
        }
        return msg;
    }

    @Override
    @Transactional
    public LocalMessage manualRetry(Long id) {
        LocalMessage msg = getById(id);
        if (msg == null) {
            throw new BusinessException(ErrorCode.LOCAL_MESSAGE_NOT_FOUND);
        }
        if (MsgStatus.SUCCESS.equals(msg.getStatus()) || MsgStatus.IGNORED.equals(msg.getStatus())) {
            throw new BusinessException(ErrorCode.LOCAL_MESSAGE_STATUS_INVALID);
        }
        lambdaUpdate()
                .eq(LocalMessage::getId, id)
                .set(LocalMessage::getStatus, MsgStatus.PENDING.name())
                .set(LocalMessage::getRetryCount, 0)
                .set(LocalMessage::getNextRetryAt, OffsetDateTime.now())
                .set(LocalMessage::getLastError, (String) null)
                .set(LocalMessage::getTraceId, null)
                // TODO: traceId完善后，要传入
                .set(LocalMessage::getUpdatedAt, OffsetDateTime.now())
                .update();
        // TODO: 消息实际消费/投递 — 后续引入 MQ (RabbitMQ/RocketMQ) 或 Redis Stream 后，
        //       此处应将消息投递到对应 topic 的队列中，由消费者异步处理
        return getById(id);
    }

    @Override
    @Transactional
    public BatchRetryResult batchRetry(List<Long> ids) {
        List<BatchRetryResult.RetryItemResult> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        for (Long id : ids) {
            try {
                manualRetry(id);
                results.add(new BatchRetryResult.RetryItemResult(id, true, null));
                successCount++;
            } catch (Exception e) {
                log.error("批量重试失败, id=[{}]", id, e);
                results.add(new BatchRetryResult.RetryItemResult(id, false, e.getMessage()));
                failCount++;
            }
        }
        return new BatchRetryResult(successCount, failCount, results);
    }

    @Override
    @Transactional
    public LocalMessage updateStatus(Long id, String status) {
        LocalMessage msg = getById(id);
        if (msg == null) {
            throw new BusinessException(ErrorCode.LOCAL_MESSAGE_NOT_FOUND);
        }
        lambdaUpdate()
                .eq(LocalMessage::getId, id)
                .set(LocalMessage::getStatus, status)
                .set(LocalMessage::getTraceId, null)
                // TODO: traceId完善后，要传入
                .set(LocalMessage::getUpdatedAt, OffsetDateTime.now())
                .update();
        // TODO: 若状态改为 PENDING，且集成了 MQ/Redis Stream，应重新投递消息到队列
        return getById(id);
    }

    @Override
    @Transactional
    public List<LocalMessage> fetchAndLockForDispatch(MsgPriority priority, int limit) {
        OffsetDateTime now = OffsetDateTime.now();
        Page<LocalMessage> page = new Page<>(0, limit);
        page.setSearchCount(false);
        List<LocalMessage> messages = lambdaQuery()
                .eq(LocalMessage::getPriority, priority.name())
                .and(w -> w.and(i -> i.eq(LocalMessage::getStatus, MsgStatus.PENDING.name())
                                .and(j -> j.isNull(LocalMessage::getNextRetryAt)
                                        .or().le(LocalMessage::getNextRetryAt, now)))
                        .or(i -> i.eq(LocalMessage::getStatus, MsgStatus.PROCESSING.name())
                                .le(LocalMessage::getUpdatedAt, now.minusSeconds(30))))
                .orderByAsc(LocalMessage::getCreatedAt)
                .last("FOR UPDATE SKIP LOCKED")
                .page(page)
                .getRecords();

        if (!messages.isEmpty()) {
            List<Long> ids = messages.stream().map(LocalMessage::getId).toList();
            lambdaUpdate()
                    .in(LocalMessage::getId, ids)
                    .set(LocalMessage::getStatus, MsgStatus.PROCESSING)
                    .set(LocalMessage::getUpdatedAt, now)
                    .update();
        }
        return messages;
    }

    @Override
    @Transactional
    public void markSuccess(Long id) {
        lambdaUpdate()
                .eq(LocalMessage::getId, id)
                .set(LocalMessage::getStatus, MsgStatus.SUCCESS)
                .set(LocalMessage::getNextRetryAt, (OffsetDateTime) null)
                .set(LocalMessage::getTraceId, null)
                // TODO: traceId完善后，要传入
                .set(LocalMessage::getUpdatedAt, OffsetDateTime.now())
                .update();
    }

    @Override
    @Transactional
    public void markFailed(Long id, int retryCount, MsgStatus newStatus, OffsetDateTime nextRetry, String lastError) {
        lambdaUpdate()
                .eq(LocalMessage::getId, id)
                .set(LocalMessage::getRetryCount, retryCount)
                .set(LocalMessage::getStatus, newStatus)
                .set(LocalMessage::getNextRetryAt, nextRetry)
                .set(LocalMessage::getLastError, lastError)
                .set(LocalMessage::getTraceId, null)
                // TODO: traceId完善后，要传入
                .set(LocalMessage::getUpdatedAt, OffsetDateTime.now())
                .update();
    }
}
