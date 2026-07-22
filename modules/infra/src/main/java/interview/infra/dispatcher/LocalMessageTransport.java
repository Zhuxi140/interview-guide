package interview.infra.dispatcher;

import interview.common.constant.MessageEnvelope;
import interview.common.enums.MsgTopic;
import interview.common.spi.MessageHandleResult;
import interview.common.spi.MessageHandler;
import interview.common.spi.MessageTransport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 当前模块化单体使用的本地消息传输实现。
 */
@Component
@ConditionalOnProperty(name = "app.message.transport", havingValue = "local", matchIfMissing = true)
public class LocalMessageTransport implements MessageTransport {

    private final Map<MsgTopic, MessageHandler> handlerMap;

    public LocalMessageTransport(List<MessageHandler> handlers) {
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(MessageHandler::getTopic, Function.identity()));
    }

    @Override
    public MessageHandleResult dispatch(MessageEnvelope message) {
        // 未注册处理器属于不可重试的协议或部署错误。
        MessageHandler handler = handlerMap.get(message.topic());
        if (handler == null) {
            return MessageHandleResult.permanentFailure("no handler for topic: " + message.topic());
        }
        return handler.handle(message);
    }
}
