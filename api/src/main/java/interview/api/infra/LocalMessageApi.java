package interview.api.infra;

import interview.api.infra.dto.MessageDTO;

/**
 * @author zhuxi
 */
public interface LocalMessageApi {

    /**
     * 在调用方当前事务中保存消息
     * @param message 消息内容
     * @return 消息 ID
     */
    Long saveInCurrentTransaction(MessageDTO message);

    /**
     * 忽略尚未开始处理的消息
     * @param messageId 消息 ID
     * @return 是否成功完成状态转换
     */
    boolean ignorePending(Long messageId);

    /**
     * 调整待处理消息的执行时间
     * @param messageId 消息 ID
     * @param executeAt 下次执行时间
     * @return 是否成功完成状态转换
     */
    boolean schedulePending(Long messageId, java.time.OffsetDateTime executeAt);

    /**
     * 按业务幂等键确保消息存在并可被调度
     * @param message 消息内容
     * @return 消息 ID
     */
    Long ensurePending(MessageDTO message);
}
