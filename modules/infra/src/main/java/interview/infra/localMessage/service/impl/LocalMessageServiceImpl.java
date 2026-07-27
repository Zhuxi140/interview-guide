package interview.infra.localMessage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.common.enums.ErrorCode;
import interview.common.enums.MsgPriority;
import interview.common.enums.MsgStatus;
import interview.common.exception.BusinessException;
import interview.infra.localMessage.mapper.LocalMessageMapper;
import interview.infra.localMessage.model.entity.LocalMessage;
import interview.infra.localMessage.model.req.LocalMessagePageReq;
import interview.infra.localMessage.service.LocalMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 本地消息查询、租约领取与状态推进服务。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocalMessageServiceImpl extends ServiceImpl<LocalMessageMapper, LocalMessage>
        implements LocalMessageService {

    @Override
    public IPage<LocalMessage> pageQuery(LocalMessagePageReq req) {
        // 使用类型安全条件构建管理端查询。
        LambdaQueryWrapper<LocalMessage> wrapper = Wrappers.lambdaQuery(LocalMessage.class)
                .eq(req.getStatus() != null, LocalMessage::getStatus, req.getStatus())
                .eq(req.getTopic() != null, LocalMessage::getTopic, req.getTopic())
                .eq(req.getPriority() != null, LocalMessage::getPriority, req.getPriority())
                .ge(req.getStartTime() != null, LocalMessage::getCreatedAt, req.getStartTime())
                .le(req.getEndTime() != null, LocalMessage::getCreatedAt, req.getEndTime())
                .orderByDesc(LocalMessage::getCreatedAt);
        return page(new Page<>(req.getPage(), req.getSize()), wrapper);
    }

    @Override
    public LocalMessage getDetail(Long id) {
        LocalMessage message = getById(id);
        if (message == null) {
            throw new BusinessException(ErrorCode.LOCAL_MESSAGE_NOT_FOUND);
        }
        return message;
    }

    @Override
    @Transactional
    public LocalMessage manualRetry(Long id) {
        // 人工操作只能恢复已经终止的失败消息，不能抢占有效租约。
        LocalMessage update = new LocalMessage();
        update.setStatus(MsgStatus.PENDING);
        update.setRetryCount(0);
        update.setNextRetryAt(OffsetDateTime.now());
        update.setLastError(null);
        update.setLeaseOwner(null);
        update.setLeaseUntil(null);
        update.setTraceId(null);
        update.setUpdatedAt(OffsetDateTime.now());

        boolean updated = update(update, Wrappers.lambdaUpdate(LocalMessage.class)
                .eq(LocalMessage::getId, id)
                .eq(LocalMessage::getStatus, MsgStatus.FAILED));
        if (!updated) {
            LocalMessage current = getById(id);
            if (current == null) {
                throw new BusinessException(ErrorCode.LOCAL_MESSAGE_NOT_FOUND);
            }
            throw new BusinessException(ErrorCode.LOCAL_MESSAGE_STATUS_INVALID);
        }
        return getById(id);
    }

    @Override
    @Transactional
    public BatchRetryResult batchRetry(List<Long> ids) {
        // 单条失败只记录结果，不中断后续消息处理。
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
    public List<LocalMessage> claimForDispatch(MsgPriority priority, String workerId,
                                               long leaseDurationSeconds, int limit) {
        // 领取事务只持有 local_message 行锁，执行器在事务提交后处理业务。
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime leaseUntil = now.plusSeconds(leaseDurationSeconds);
        return baseMapper.claimForDispatch(priority.name(), workerId, now, leaseUntil, limit);
    }

    @Override
    @Transactional
    public boolean markSuccessIfOwned(LocalMessage lease) {
        LocalMessage update = terminalUpdate(MsgStatus.SUCCESS, null);
        return updateOwned(lease, update);
    }

    @Override
    @Transactional
    public boolean markIgnoredIfOwned(LocalMessage lease, String reason) {
        LocalMessage update = terminalUpdate(MsgStatus.IGNORED, conciseError(reason));
        return updateOwned(lease, update);
    }

    @Override
    @Transactional
    public boolean markRetryIfOwned(LocalMessage lease, int retryCount, MsgStatus newStatus,
                                    OffsetDateTime nextRetry, String lastError) {
        // 重试与永久失败都必须校验租约版本，防止旧执行者覆盖新结果。
        LocalMessage update = new LocalMessage();
        update.setRetryCount(retryCount);
        update.setStatus(newStatus);
        update.setNextRetryAt(newStatus == MsgStatus.PENDING ? nextRetry : null);
        update.setLastError(conciseError(lastError));
        update.setLeaseOwner(null);
        update.setLeaseUntil(null);
        update.setTraceId(null);
        update.setUpdatedAt(OffsetDateTime.now());
        return updateOwned(lease, update);
    }

    private LocalMessage terminalUpdate(MsgStatus status, String reason) {
        LocalMessage update = new LocalMessage();
        update.setStatus(status);
        update.setNextRetryAt(null);
        update.setLastError(reason);
        update.setLeaseOwner(null);
        update.setLeaseUntil(null);
        update.setTraceId(null);
        update.setUpdatedAt(OffsetDateTime.now());
        return update;
    }

    private boolean updateOwned(LocalMessage lease, LocalMessage update) {
        return update(update, Wrappers.lambdaUpdate(LocalMessage.class)
                .eq(LocalMessage::getId, lease.getId())
                .eq(LocalMessage::getStatus, MsgStatus.PROCESSING)
                .eq(LocalMessage::getLeaseOwner, lease.getLeaseOwner())
                .eq(LocalMessage::getLeaseVersion, lease.getLeaseVersion()));
    }

    private String conciseError(String error) {
        if (error == null || error.length() <= 1000) {
            return error;
        }
        return error.substring(0, 1000);
    }
}
