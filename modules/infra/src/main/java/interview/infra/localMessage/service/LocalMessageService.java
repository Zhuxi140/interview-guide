package interview.infra.localMessage.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.common.enums.MsgPriority;
import interview.common.enums.MsgStatus;
import interview.infra.localMessage.model.entity.LocalMessage;

import java.time.OffsetDateTime;
import java.util.List;

public interface LocalMessageService extends IService<LocalMessage> {

    IPage<LocalMessage> pageQuery(Integer page, Integer size, String status, String topic, String priority,
                                  OffsetDateTime startTime, OffsetDateTime endTime);

    LocalMessage getDetail(Long id);

    LocalMessage manualRetry(Long id);

    BatchRetryResult batchRetry(List<Long> ids);

    LocalMessage updateStatus(Long id, String status);

    List<LocalMessage> fetchAndLockForDispatch(MsgPriority priority, int limit);

    void markSuccess(Long id);

    void markFailed(Long id, int retryCount, MsgStatus newStatus, OffsetDateTime nextRetry, String lastError);

    record BatchRetryResult(int successCount, int failCount, List<RetryItemResult> results) {
        public record RetryItemResult(Long id, boolean success, String error) {}
    }
}
