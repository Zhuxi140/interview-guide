package interview.textinterview.ws;

import java.time.OffsetDateTime;
import java.util.UUID;

import cn.hutool.json.JSONObject;

/**
 * 面试实时通道统一事件信封。
 *
 * <p>契约固定为 {@code { eventId, sequence, type, payload, serverTime }}，
 * 文本面试与语音面试共用同一信封与同一端点（见第三阶段接口文档「实时协议」）。
 * 序列化为 JSON 时使用 Hutool 显式建对象而非反射序列化：record 的访问器不满足
 * JavaBean 约定，依赖反射的序列化器行为不可控。</p>
 *
 * <p>{@code sequence} 仅持久化语义事件才有值（取自
 * {@code interview_timeline_events.sequence_num}）；心跳、增量文本、错误等瞬时事件
 * 传 {@code null} 且不占用序号，此时该字段不会出现在报文中。</p>
 *
 * @param eventId    会话内事件幂等 ID
 * @param sequence   持久化事件序号；瞬时事件为 null
 * @param type       事件类型，如 {@code question.completed} / {@code answer.accepted}
 * @param payload    事件载荷，结构由 {@code type} 决定
 * @param serverTime 服务端产生时间
 */
public record InterviewEventEnvelope(
        String eventId,
        Long sequence,
        String type,
        Object payload,
        OffsetDateTime serverTime
) {

    /**
     * 构造持久化语义事件信封：序号由调用方从会话时间线取得。
     *
     * @param sequence 会话内严格递增的持久化序号
     * @param type     事件类型
     * @param payload  事件载荷
     * @return 事件信封
     */
    public static InterviewEventEnvelope persisted(long sequence, String type, Object payload) {
        return new InterviewEventEnvelope(newEventId(), sequence, type, payload, OffsetDateTime.now());
    }

    /**
     * 构造瞬时事件信封：心跳、流式增量、错误回执等，不落时间线、不占序号。
     *
     * @param type    事件类型
     * @param payload 事件载荷
     * @return 事件信封
     */
    public static InterviewEventEnvelope ephemeral(String type, Object payload) {
        return new InterviewEventEnvelope(newEventId(), null, type, payload, OffsetDateTime.now());
    }

    /**
     * 序列化为下发给客户端的 JSON 报文。
     *
     * @return JSON 字符串
     */
    public String toJson() {
        JSONObject json = new JSONObject();
        json.set("eventId", eventId);
        json.set("sequence", sequence);
        json.set("type", type);
        json.set("payload", payload);
        json.set("serverTime", serverTime == null ? null : serverTime.toString());
        return json.toString();
    }

    private static String newEventId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
