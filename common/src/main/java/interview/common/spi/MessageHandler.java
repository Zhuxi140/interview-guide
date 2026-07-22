package interview.common.spi;

import interview.common.constant.MessageEnvelope;
import interview.common.enums.MsgTopic;

public interface MessageHandler {

    /**
     * 获取处理器主题
     * @return 消息主题
     */
    MsgTopic getTopic();

    /**
     * 处理消息
     * @param message 消息信封
     * @return 处理结果
     */
    MessageHandleResult handle(MessageEnvelope message);
}
