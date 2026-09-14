package interview.textinterview.ws;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import cn.hutool.core.util.StrUtil;
import interview.common.constant.InterviewConnectionContext;
import interview.textinterview.service.InterviewSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 面试实时通道握手拦截器：以一次性连接凭证完成鉴权，取代 JWT 拦截器。
 *
 * <p>该路径已在 {@code WebMvcConfig} 中从 JWT 拦截器范围排除——浏览器无法为
 * WebSocket 握手附加 {@code Authorization} 头。凭证通过 {@code Sec-WebSocket-Protocol}
 * 子协议头传入（客户端以 {@code join-token} 返回的 connectionToken 作为子协议名连接），
 * 服务端消费一次即失效，天然防重放。</p>
 *
 * <p><b>必须回显子协议</b>：按 WebSocket 规范，若服务端未在响应中选定客户端提供的某个
 * 子协议，浏览器会直接判定握手失败并关闭连接。因此这里把收到的凭证原样回写。
 * 若后续接入网关/反代剥离该响应头，需改为查询参数传递凭证。</p>
 *
 * @author zhuxi
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewHandshakeInterceptor implements HandshakeInterceptor {

    /** 挂到 {@code WebSocketSession} 属性上的连接上下文 key。 */
    public static final String ATTR_CONNECTION_CONTEXT = "interview.connectionContext";

    /** 挂到 {@code WebSocketSession} 属性上的会话 ID key。 */
    public static final String ATTR_SESSION_ID = "interview.sessionId";

    private static final String SUB_PROTOCOL_HEADER = "Sec-WebSocket-Protocol";

    private final InterviewSessionService interviewSessionService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = resolveToken(request);
        if (StrUtil.isBlank(token)) {
            log.warn("WebSocket 握手被拒：缺少连接凭证。uri={}", request.getURI());
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }

        // 原子消费一次性凭证，同时完成鉴权与防重放。
        InterviewConnectionContext context = interviewSessionService.consumeConnectionToken(token);
        if (context == null) {
            log.warn("WebSocket 握手被拒：连接凭证无效或已被使用。uri={}", request.getURI());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        // 路径会话与凭证会话必须一致，避免拿 A 会话的凭证连 B 会话。
        Long pathSessionId = pathSessionId(request);
        if (pathSessionId != null && !pathSessionId.equals(context.getSessionId())) {
            log.warn("WebSocket 握手被拒：路径会话与凭证不匹配。pathSessionId={}, tokenSessionId={}",
                    pathSessionId, context.getSessionId());
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }

        attributes.put(ATTR_CONNECTION_CONTEXT, context);
        attributes.put(ATTR_SESSION_ID, context.getSessionId());
        response.getHeaders().set(SUB_PROTOCOL_HEADER, token);
        log.info("WebSocket 握手通过: sessionId={}, userId={}, role={}",
                context.getSessionId(), context.getUserId(), context.getRole());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.warn("WebSocket 握手过程异常。uri={}", request.getURI(), exception);
        }
    }

    /**
     * 取子协议头中的第一个取值作为连接凭证。
     * 客户端多传子协议时以第一个为准，约定即「凭证在前、扩展协议在后」。
     */
    private String resolveToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(SUB_PROTOCOL_HEADER);
        if (StrUtil.isBlank(header)) {
            return null;
        }
        return StrUtil.split(header, ',').stream()
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElse(null);
    }

    /** 从路径末段解析会话 ID；非数字时返回 null，以凭证携带的会话 ID 为权威来源。 */
    private Long pathSessionId(ServerHttpRequest request) {
        String path = StrUtil.removeSuffix(request.getURI().getPath(), "/");
        String lastSegment = StrUtil.subAfter(path, "/", true);
        return StrUtil.isNumeric(lastSegment) ? Long.valueOf(lastSegment) : null;
    }
}
