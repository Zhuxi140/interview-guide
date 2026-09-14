package interview.textinterview.ws;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import interview.common.constant.InterviewConnectionContext;
import interview.common.enums.UserType;
import interview.framework.context.AuthContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 面试实时通道处理器（U3-01）。
 *
 * <p>连接生命周期（登记 / 摘除 / 认证上下文重建）已实现；
 * <b>入站消息分发与会话状态机为 TODO，由开发者补全</b>。</p>
 *
 * @author zhuxi
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewWebSocketHandler extends TextWebSocketHandler {

    private final InterviewSessionConnectionRegistry connectionRegistry;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        InterviewConnectionContext context = connectionContext(session);
        if (context == null) {
            // 理论不可达：握手拦截器已拒绝无上下文的连接。保留兜底避免脏连接进入注册表。
            log.error("连接缺少握手上下文，直接关闭: wsSessionId={}", session.getId());
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("缺少连接上下文"));
            return;
        }
        connectionRegistry.register(context.getSessionId(), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        InterviewConnectionContext context = connectionContext(session);
        if (context == null) {
            closeQuietly(session, CloseStatus.NOT_ACCEPTABLE.withReason("缺少连接上下文"));
            return;
        }

        // WS 消息线程没有 JwtInterceptor 写入的 AuthContext；不重建的话，
        // 任何复用 AuthContext 的用例 Service（如 InterviewAnswerService#submitAnswer、
        // InterviewSessionService#readySession）都会抛未授权异常。
        // 分发结束必须清理：容器线程池会复用线程，残留身份会泄漏给下一个连接的消息。
        try {
            AuthContext.setAuthContext(toAuthUser(context));

            // ==================== TODO(你来实现) U3-01 入站消息分发 ====================
            // 入站报文同样使用 InterviewEventEnvelope 信封（{type, payload}），
            // 用 JSONUtil.parseObj(message.getPayload()) 解析后按 type 分发。
            // C→S 事件契约（见第三阶段接口文档「文本面试消息契约」）：
            //   client.ready        {}                                            就绪确认
            //   answer.submit       {questionId, content, elapsedSeconds?, idempotencyKey}
            //   interviewer.message {content}                                     面试官插话
            //   heartbeat           {}                                            保活
            //   reconnect           {lastSequence}                                断线补发
            //
            // 关键约束（这些是设计决策，故留给你）：
            //   a) client.ready 与 interviewer.message 只在对应前置状态下合法；
            //      answer.submit 复用 InterviewAnswerService#submitAnswer，
            //      幂等键取载荷中的 idempotencyKey，重连重放才能命中。
            //   b) interviewer.message 必须校验当前存在 ACTIVE 接管记录
            //      （见 InterviewTakeoverService），否则面试官可绕过接管直接插话。
            //   c) reconnect 的补发语义：先按 lastSequence 从
            //      InterviewSessionService#getTimeline 取缺失的持久化事件逐条重放，
            //      再恢复实时流；补发用的 sequence 必须是时间线真实序号，不可新分配。
            //   d) heartbeat 只刷新活跃时间，绝不推进任何业务状态机。
            //   e) 分发过程中的业务异常应回下行一条 error 信封（{code, message}），
            //      且不占用持久化序号（用 InterviewEventEnvelope.ephemeral），
            //      不要直接抛出让容器断连——客户端会误判为网络故障。
            //   f) 未知 type 一律回 error 并记日志，不要静默丢弃。
            //
            // 下行出口统一走 connectionRegistry.broadcast(sessionId, envelope)；
            // 持久化事件的 sequence 必须取自 interview_timeline_events.sequence_num，
            // 增量文本（question.delta）与心跳用 ephemeral，不落时间线。
            //
            // 另外两件尚未实现的基础能力（可先不做，但要知道缺口）：
            //   - 心跳超时清理：需要 maxSessionIdleTimeout（ServletServerContainerFactoryBean）
            //     或自建定时任务扫描，光靠 heartbeat 报文本身不会断开僵死连接。
            //   - 单连接并发发送：registry 内已对 sendMessage 加锁，分发时无需再处理。
            // ======================================================================
        } finally {
            AuthContext.remove();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        InterviewConnectionContext context = connectionContext(session);
        if (context != null) {
            connectionRegistry.unregister(context.getSessionId(), session);
        }
        log.info("面试实时连接已关闭: wsSessionId={}, code={}, reason={}",
                session.getId(), status.getCode(), status.getReason());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("面试实时连接传输异常: wsSessionId={}", session.getId(), exception);
        closeQuietly(session, CloseStatus.SERVER_ERROR);
    }

    /** 从会话属性读取握手阶段挂入的连接上下文。 */
    private InterviewConnectionContext connectionContext(WebSocketSession session) {
        return (InterviewConnectionContext) session.getAttributes()
                .get(InterviewHandshakeInterceptor.ATTR_CONNECTION_CONTEXT);
    }

    /**
     * 按连接上下文重建认证上下文。
     *
     * <p>不设置 riskLevel：风控校验只发生在 HTTP 拦截器链，WS 分发不走该链路；
     * 留空可让误用到风控的调用按未授权失败（fail-closed），而不是被伪造的等级放行。</p>
     */
    private AuthContext.AuthUser toAuthUser(InterviewConnectionContext context) {
        return AuthContext.AuthUser.builder()
                .userId(context.getUserId())
                .userType(UserType.valueOf(context.getUserType()))
                .enterpriseId(context.getEnterpriseId())
                .platformRoleCodes(List.of())
                .entRoleMap(Map.of())
                .build();
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException exception) {
            log.debug("关闭实时连接失败: wsSessionId={}", session.getId(), exception);
        }
    }
}
