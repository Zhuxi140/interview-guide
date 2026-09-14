package interview.textinterview.ws;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import lombok.extern.slf4j.Slf4j;

/**
 * 面试会话实时连接注册表：维护 {@code sessionId → 该会话的在线连接集合}。
 *
 * <p>上行用于握手成功后登记连接、断开时摘除；下行是向会话推送实时事件的唯一出口，
 * 供题目下发、接管指令（{@code AI_PAUSE} / {@code AI_RESUME}）等场景复用。</p>
 *
 * <p><b>当前为单节点内存实现</b>：仅覆盖单实例部署。多实例横向扩展时需要把下发
 * 改为「广播到所有实例」的形式（Redis Pub/Sub 或消息队列），否则连在实例 A 上的
 * 候选人收不到由实例 B 处理的事件。这一点在引入多实例前必须替换。</p>
 *
 * @author zhuxi
 */
@Slf4j
@Component
public class InterviewSessionConnectionRegistry {

    private final Map<Long, Map<String, WebSocketSession>> sessionConnections = new ConcurrentHashMap<>();

    /**
     * 登记一条会话连接。
     *
     * @param sessionId 面试会话 ID
     * @param session   WebSocket 连接
     */
    public void register(Long sessionId, WebSocketSession session) {
        sessionConnections
                .computeIfAbsent(sessionId, key -> new ConcurrentHashMap<>())
                .put(session.getId(), session);
        log.info("面试实时连接已登记: sessionId={}, wsSessionId={}, 当前连接数={}",
                sessionId, session.getId(), connectionCount(sessionId));
    }

    /**
     * 摘除一条会话连接；该会话已无连接时同时清理外层条目。
     *
     * @param sessionId 面试会话 ID
     * @param session   WebSocket 连接
     */
    public void unregister(Long sessionId, WebSocketSession session) {
        // computeIfPresent 保证「摘除 + 空集合清理」对该会话是原子操作，
        // 避免与并发 register 竞争后误删刚登记进来的连接。
        sessionConnections.computeIfPresent(sessionId, (key, connections) -> {
            connections.remove(session.getId());
            return connections.isEmpty() ? null : connections;
        });
        log.info("面试实时连接已摘除: sessionId={}, wsSessionId={}, 剩余连接数={}",
                sessionId, session.getId(), connectionCount(sessionId));
    }

    /**
     * 向会话内所有在线连接广播一条事件。
     *
     * <p>无在线连接时按丢弃处理并记日志：实时推送是尽力而为的旁路，
     * 权威状态始终以库中会话状态与时间线为准，客户端重连后可通过
     * {@code reconnect} 补发流程找回缺失事件。</p>
     *
     * @param sessionId 面试会话 ID
     * @param envelope  事件信封
     */
    public void broadcast(Long sessionId, InterviewEventEnvelope envelope) {
        Map<String, WebSocketSession> connections = sessionConnections.get(sessionId);
        if (connections == null || connections.isEmpty()) {
            log.debug("会话当前无在线连接，实时事件丢弃: sessionId={}, type={}",
                    sessionId, envelope.type());
            return;
        }
        String text = envelope.toJson();
        for (WebSocketSession session : connections.values()) {
            sendQuietly(sessionId, session, text);
        }
    }

    /**
     * 查询会话当前在线连接数。
     *
     * @param sessionId 面试会话 ID
     * @return 在线连接数；无连接时为 0
     */
    public int connectionCount(Long sessionId) {
        Map<String, WebSocketSession> connections = sessionConnections.get(sessionId);
        return connections == null ? 0 : connections.size();
    }

    /**
     * 单条连接发送：WebSocketSession 并非并发安全，同一连接的多线程发送必须串行化。
     * 发送失败只记日志，连接状态交由容器在 {@code afterConnectionClosed} 中收尾。
     */
    private void sendQuietly(Long sessionId, WebSocketSession session, String text) {
        synchronized (session) {
            if (!session.isOpen()) {
                return;
            }
            try {
                session.sendMessage(new TextMessage(text));
            } catch (IOException exception) {
                log.warn("实时事件下发失败: sessionId={}, wsSessionId={}, type={}",
                        sessionId, session.getId(), text, exception);
            }
        }
    }
}
