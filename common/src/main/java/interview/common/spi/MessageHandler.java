package interview.common.spi;

import interview.common.constant.Message;
import interview.common.enums.MsgTopic;

public interface MessageHandler {
    MsgTopic getTopic();
    void handle(Message msg);
}
