package interview.common.constant;

import interview.common.enums.MsgTopic;
import lombok.Builder;

/**
 * 本地消息及未来 MQ 共用的消息信封。
 *
 * @param messageId 消息 ID，同时作为幂等键
 * @param topic 消息主题
 * @param schemaVersion 消息协议版本
 * @param payload 业务载荷
 * @param traceId 调用链 ID
 */
@Builder
public record MessageEnvelope(
        Long messageId,
        MsgTopic topic,
        Integer schemaVersion,
        String payload,
        String traceId
) {
}
