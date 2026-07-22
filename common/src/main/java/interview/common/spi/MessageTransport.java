package interview.common.spi;

import interview.common.constant.MessageEnvelope;

/**
 * 本地处理器与未来 MQ 发布器共用的传输边界。
 */
public interface MessageTransport {

    /**
     * 投递消息
     * @param message 消息信封
     * @return 投递或本地处理结果
     */
    MessageHandleResult dispatch(MessageEnvelope message);
}
