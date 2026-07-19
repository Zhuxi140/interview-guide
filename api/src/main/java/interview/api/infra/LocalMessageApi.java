package interview.api.infra;

import interview.api.infra.dto.MessageDTO;

/**
 * @author zhuxi
 */
public interface LocalMessageApi {

    void saveMsg(MessageDTO dto);

    void saveMsgNewTransaction(MessageDTO dto);
}
