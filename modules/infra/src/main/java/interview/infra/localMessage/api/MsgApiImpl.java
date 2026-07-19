package interview.infra.localMessage.api;

import interview.api.infra.LocalMessageApi;
import interview.api.infra.dto.MessageDTO;
import interview.common.exception.BusinessException;
import interview.infra.localMessage.model.entity.LocalMessage;
import interview.infra.localMessage.service.LocalMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author zhuxi
 */

@Service
@RequiredArgsConstructor
public class MsgApiImpl implements LocalMessageApi {

    private final LocalMessageService localMessageService;


    @Override
    public void saveMsg(MessageDTO dto) {
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
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveMsgNewTransaction(MessageDTO dto) {
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
    }
}
