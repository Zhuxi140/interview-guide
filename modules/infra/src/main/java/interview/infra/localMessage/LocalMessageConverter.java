package interview.infra.localMessage;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.infra.localMessage.model.entity.LocalMessage;
import interview.infra.localMessage.model.vo.LocalMessageDetailVO;
import interview.infra.localMessage.model.vo.LocalMessagePageVO;
import interview.infra.localMessage.model.vo.LocalMessageRetryVO;
import org.springframework.stereotype.Component;

/**
 * 本地消息管理端响应转换器。
 */
@Component
public class LocalMessageConverter {

    public IPage<LocalMessagePageVO> toPageVO(IPage<LocalMessage> source) {
        Page<LocalMessagePageVO> target =
                new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        target.setRecords(source.getRecords().stream().map(this::toPageVO).toList());
        return target;
    }

    public LocalMessagePageVO toPageVO(LocalMessage message) {
        return LocalMessagePageVO.builder()
                .id(message.getId())
                .topic(message.getTopic().name())
                .bizKey(message.getBizKey())
                .priority(message.getPriority().name())
                .status(message.getStatus().name())
                .retryCount(message.getRetryCount())
                .maxRetries(message.getMaxRetries())
                .nextRetryAt(message.getNextRetryAt())
                .leaseUntil(message.getLeaseUntil())
                .lastError(message.getLastError())
                .createdAt(message.getCreatedAt())
                .build();
    }

    public LocalMessageDetailVO toDetailVO(LocalMessage message) {
        return LocalMessageDetailVO.builder()
                .id(message.getId())
                .topic(message.getTopic().name())
                .bizKey(message.getBizKey())
                .schemaVersion(message.getSchemaVersion())
                .payload(message.getPayload())
                .priority(message.getPriority().name())
                .status(message.getStatus().name())
                .retryCount(message.getRetryCount())
                .maxRetries(message.getMaxRetries())
                .nextRetryAt(message.getNextRetryAt())
                .retryHistory(message.getRetryHistory())
                .lastError(message.getLastError())
                .leaseOwner(message.getLeaseOwner())
                .leaseUntil(message.getLeaseUntil())
                .leaseVersion(message.getLeaseVersion())
                .traceId(message.getTraceId())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }

    public LocalMessageRetryVO toRetryVO(LocalMessage message) {
        return new LocalMessageRetryVO(
                message.getId(), message.getStatus(), message.getNextRetryAt());
    }
}
