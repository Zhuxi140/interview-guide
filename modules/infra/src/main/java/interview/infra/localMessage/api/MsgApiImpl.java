package interview.infra.localMessage.api;

import interview.api.infra.LocalMessageApi;
import interview.api.infra.dto.MessageDTO;
import interview.common.enums.MsgStatus;
import interview.common.exception.BusinessException;
import interview.infra.localMessage.model.entity.LocalMessage;
import interview.infra.localMessage.service.LocalMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 */

@Service
@RequiredArgsConstructor
public class MsgApiImpl implements LocalMessageApi {

    private final LocalMessageService localMessageService;


    @Override
    public Long saveMsg(MessageDTO dto) {
        LocalMessage msg = LocalMessage.builder()
                .topic(dto.getTopic())
                .payload(dto.getPayload())
                .priority(dto.getPriority())
                .status(dto.getStatus())
                .retryCount(dto.getRetryCount())
                .maxRetries(dto.getMaxRetries())
                .nextRetryAt(dto.getNextRetryAt())
                .retryHistory(dto.getRetryHistory())
                .lastError(dto.getLastError())
                .build();
        localMessageService.save(msg);
        return msg.getId();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long saveMsgNewTransaction(MessageDTO dto) {
        LocalMessage msg = LocalMessage.builder()
                .topic(dto.getTopic())
                .payload(dto.getPayload())
                .priority(dto.getPriority())
                .status(dto.getStatus())
                .retryCount(dto.getRetryCount())
                .maxRetries(dto.getMaxRetries())
                .nextRetryAt(dto.getNextRetryAt())
                .retryHistory(dto.getRetryHistory())
                .lastError(dto.getLastError())
                .build();
        localMessageService.save(msg);
        return msg.getId();
    }

    @Override
    @Transactional
    public void updateStatus(Long msgId, MsgStatus status) {
        localMessageService.lambdaUpdate()
                .eq(LocalMessage::getId, msgId)
                .set(LocalMessage::getStatus, status)
                //TODO: 后续traceId完善后 补全
                .set(LocalMessage::getTraceId,null)
                .set(LocalMessage::getUpdatedAt, OffsetDateTime.now())
                .update();
    }


}
