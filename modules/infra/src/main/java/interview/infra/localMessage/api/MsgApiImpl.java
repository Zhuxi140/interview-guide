package interview.infra.localMessage.api;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import interview.api.infra.LocalMessageApi;
import interview.api.infra.dto.MessageDTO;
import interview.common.enums.MsgPriority;
import interview.common.enums.MsgStatus;
import interview.common.util.TraceUtil;
import interview.infra.localMessage.model.entity.LocalMessage;
import interview.infra.localMessage.service.LocalMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 本地消息跨模块接口实现。
 */
@Service
@RequiredArgsConstructor
public class MsgApiImpl implements LocalMessageApi {

    private static final int DEFAULT_MAX_RETRIES = 5;
    private static final int DEFAULT_SCHEMA_VERSION = 1;

    private final LocalMessageService localMessageService;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Long saveInCurrentTransaction(MessageDTO message) {
        // Outbox 必须与调用方业务数据处于同一数据库事务。
        LocalMessage localMessage = toEntity(message);
        localMessageService.save(localMessage);
        return localMessage.getId();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean ignorePending(Long messageId) {
        // 只取消尚未领取的消息，避免与已经开始执行的工作节点竞态。
        LocalMessage update = new LocalMessage();
        update.setStatus(MsgStatus.IGNORED);
        update.setNextRetryAt(null);
        update.setLastError("业务处理成功，取消上传补偿");
        update.setTraceId(TraceUtil.getTraceId());
        update.setUpdatedAt(OffsetDateTime.now());
        return localMessageService.update(update, Wrappers.lambdaUpdate(LocalMessage.class)
                .eq(LocalMessage::getId, messageId)
                .eq(LocalMessage::getStatus, MsgStatus.PENDING));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean schedulePending(Long messageId, OffsetDateTime executeAt) {
        // 上传失败后只允许提前尚未领取消息的执行时间。
        LocalMessage update = new LocalMessage();
        update.setNextRetryAt(executeAt);
        update.setTraceId(TraceUtil.getTraceId());
        update.setUpdatedAt(OffsetDateTime.now());
        return localMessageService.update(update, Wrappers.lambdaUpdate(LocalMessage.class)
                .eq(LocalMessage::getId, messageId)
                .eq(LocalMessage::getStatus, MsgStatus.PENDING));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Long ensurePending(MessageDTO message) {
        // 先按业务幂等键寻找原消息，避免修复任务重复插入。
        LocalMessage existing = localMessageService.lambdaQuery()
                .eq(LocalMessage::getTopic, message.getTopic())
                .eq(LocalMessage::getBizKey, message.getBizKey())
                .one();
        if (existing == null) {
            return saveInCurrentTransaction(message);
        }

        // 待执行消息只允许提前，失败消息则恢复为新的待执行周期。
        if (existing.getStatus() == MsgStatus.PENDING
                && message.getNextRetryAt() != null
                && (existing.getNextRetryAt() == null
                || message.getNextRetryAt().isBefore(existing.getNextRetryAt()))) {
            schedulePending(existing.getId(), message.getNextRetryAt());
        } else if (existing.getStatus() == MsgStatus.FAILED) {
            LocalMessage update = new LocalMessage();
            update.setStatus(MsgStatus.PENDING);
            update.setRetryCount(0);
            update.setNextRetryAt(message.getNextRetryAt());
            update.setLastError(message.getLastError());
            update.setLeaseOwner(null);
            update.setLeaseUntil(null);
            update.setTraceId(TraceUtil.getTraceId());
            update.setUpdatedAt(OffsetDateTime.now());
            localMessageService.update(update, Wrappers.lambdaUpdate(LocalMessage.class)
                    .eq(LocalMessage::getId, existing.getId())
                    .eq(LocalMessage::getStatus, MsgStatus.FAILED));
        }
        return existing.getId();
    }

    private LocalMessage toEntity(MessageDTO message) {
        return LocalMessage.builder()
                .topic(message.getTopic())
                .bizKey(message.getBizKey())
                .schemaVersion(message.getSchemaVersion() == null
                        ? DEFAULT_SCHEMA_VERSION : message.getSchemaVersion())
                .payload(message.getPayload())
                .priority(message.getPriority() == null ? MsgPriority.MEDIUM : message.getPriority())
                .status(message.getStatus() == null ? MsgStatus.PENDING : message.getStatus())
                .retryCount(message.getRetryCount() == null ? 0 : message.getRetryCount())
                .maxRetries(message.getMaxRetries() == null ? DEFAULT_MAX_RETRIES : message.getMaxRetries())
                .nextRetryAt(message.getNextRetryAt())
                .retryHistory(message.getRetryHistory())
                .lastError(message.getLastError())
                .leaseVersion(0L)
                .build();
    }
}
