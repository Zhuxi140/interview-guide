package interview.textinterview.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

@ExtendWith(MockitoExtension.class)
class InterviewSessionConnectionRegistryTest {

    private static final long SESSION_ID = 9001L;

    private InterviewSessionConnectionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new InterviewSessionConnectionRegistry();
    }

    @Test
    void shouldBroadcastEnvelopeToEveryConnectionOfSession() throws Exception {
        WebSocketSession first = openedSession("ws-1");
        WebSocketSession second = openedSession("ws-2");
        registry.register(SESSION_ID, first);
        registry.register(SESSION_ID, second);
        assertEquals(2, registry.connectionCount(SESSION_ID));

        registry.broadcast(SESSION_ID, InterviewEventEnvelope.persisted(
                3L, "question.completed", Map.of("content", "请介绍一次性能优化经历")));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(first).sendMessage(captor.capture());
        verify(second).sendMessage(any(TextMessage.class));
        JSONObject json = JSONUtil.parseObj(captor.getValue().getPayload());
        assertEquals(3L, json.getLong("sequence"));
        assertEquals("question.completed", json.getStr("type"));
        assertEquals("请介绍一次性能优化经历",
                json.getJSONObject("payload").getStr("content"));
        assertEquals(32, json.getStr("eventId").length());
    }

    @Test
    void shouldOmitSequenceForEphemeralEnvelope() {
        // 瞬时事件不占用持久化序号，序列化后不应出现 sequence 字段。
        JSONObject json = JSONUtil.parseObj(InterviewEventEnvelope
                .ephemeral("question.delta", Map.of("deltaText", "请")).toJson());

        assertEquals("question.delta", json.getStr("type"));
        assertFalse(json.containsKey("sequence"));
    }

    @Test
    void shouldSkipBroadcastWhenSessionHasNoConnection() {
        // 无在线连接时静默丢弃，不抛异常（实时推送是尽力而为的旁路）。
        registry.broadcast(SESSION_ID, InterviewEventEnvelope.ephemeral("heartbeat", Map.of()));

        assertEquals(0, registry.connectionCount(SESSION_ID));
    }

    @Test
    void shouldNotSendToClosedConnection() throws Exception {
        WebSocketSession closed = mock(WebSocketSession.class);
        when(closed.getId()).thenReturn("ws-closed");
        when(closed.isOpen()).thenReturn(false);
        registry.register(SESSION_ID, closed);

        registry.broadcast(SESSION_ID, InterviewEventEnvelope.ephemeral("heartbeat", Map.of()));

        verify(closed, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void shouldDropSessionEntryAfterLastConnectionUnregistered() {
        WebSocketSession first = session("ws-1");
        WebSocketSession second = session("ws-2");
        registry.register(SESSION_ID, first);
        registry.register(SESSION_ID, second);

        registry.unregister(SESSION_ID, first);
        assertEquals(1, registry.connectionCount(SESSION_ID));

        registry.unregister(SESSION_ID, second);
        assertEquals(0, registry.connectionCount(SESSION_ID));
    }

    private WebSocketSession session(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        return session;
    }

    private WebSocketSession openedSession(String id) {
        WebSocketSession session = session(id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }
}
