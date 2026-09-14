package interview.common.constant;

import lombok.Builder;
import lombok.Data;

/**
 * 面试连接凭证携带的业务上下文。
 *
 * <p>该对象随一次性凭证存入 Redis（60 秒 TTL），在 WebSocket 握手时取出并挂到
 * 会话属性上。由于 WS 消息线程没有 {@code JwtInterceptor} 写入的认证上下文，
 * 这里必须携带足够的身份信息以便消息分发前重建 <b>userId / userType / enterpriseId</b>。
 * 不携带 riskLevel：风控校验只发生在 HTTP 拦截器链，WS 消息分发不走该链路，
 * 未携带时重建出的上下文在需要风控处会按未授权失败（fail-closed）。</p>
 *
 * @author zhuxi
 */
@Data
@Builder
public class InterviewConnectionContext {

    private Long userId;
    private Long sessionId;
    private Long scheduleId;
    private Long enterpriseId;
    /** 会话内角色：CANDIDATE / INTERVIEWER */
    private String role;
    /** 账号类型，取值同 {@code UserType.name()}，供消息分发重建认证上下文使用 */
    private String userType;
}