package interview.api.infra;

import interview.api.infra.dto.MessageDTO;
import interview.common.enums.MsgStatus;

/**
 * @author zhuxi
 */
public interface LocalMessageApi {

    Long saveMsg(MessageDTO dto);

    Long saveMsgNewTransaction(MessageDTO dto);

    void updateStatus(Long msgId, MsgStatus status);
}
