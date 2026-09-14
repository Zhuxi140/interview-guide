package interview.textinterview.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import interview.common.constant.InterviewConnectionContext;
import interview.textinterview.service.InterviewSessionService;

@ExtendWith(MockitoExtension.class)
class InterviewHandshakeInterceptorTest {

    private static final long SESSION_ID = 9001L;
    private static final String TOKEN = "b7f1c2d3e4a5b6c7d8e9f0a1b2c3d4e5";
    private static final String SUB_PROTOCOL = "Sec-WebSocket-Protocol";

    @Mock
    private InterviewSessionService interviewSessionService;

    private InterviewHandshakeInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new InterviewHandshakeInterceptor(interviewSessionService);
    }

    @Test
    void shouldRejectHandshakeWithoutConnectionToken() {
        // 缺少子协议头：没有凭证就不该建立连接。
        ServerHttpResponse response = mockResponse();
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = interceptor.beforeHandshake(
                request(null), response, null, attributes);

        assertFalse(accepted);
        verify(response).setStatusCode(HttpStatus.BAD_REQUEST);
        assertTrue(attributes.isEmpty());
    }

    @Test
    void shouldRejectHandshakeWhenTokenAlreadyConsumed() {
        // 凭证无效或已被消费（一次性）：按未授权拒绝。
        ServerHttpResponse response = mockResponse();
        when(interviewSessionService.consumeConnectionToken(TOKEN)).thenReturn(null);

        boolean accepted = interceptor.beforeHandshake(
                request(TOKEN), response, null, new HashMap<>());

        assertFalse(accepted);
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldRejectWhenPathSessionDiffersFromTokenSession() {
        // 拿 A 会话的凭证连 B 会话：必须拒绝，避免越权进入其他会话频道。
        ServerHttpResponse response = mockResponse();
        when(interviewSessionService.consumeConnectionToken(TOKEN))
                .thenReturn(context(9999L));

        boolean accepted = interceptor.beforeHandshake(
                request(TOKEN), response, null, new HashMap<>());

        assertFalse(accepted);
        verify(response).setStatusCode(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldBindContextAndEchoSubProtocolOnSuccess() {
        // 成功路径：上下文挂到会话属性，并回显子协议（否则浏览器会判握手失败）。
        HttpHeaders responseHeaders = new HttpHeaders();
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        when(response.getHeaders()).thenReturn(responseHeaders);
        InterviewConnectionContext context = context(SESSION_ID);
        when(interviewSessionService.consumeConnectionToken(TOKEN)).thenReturn(context);
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = interceptor.beforeHandshake(
                request(TOKEN), response, null, attributes);

        assertTrue(accepted);
        assertSame(context,
                attributes.get(InterviewHandshakeInterceptor.ATTR_CONNECTION_CONTEXT));
        assertEquals(SESSION_ID,
                attributes.get(InterviewHandshakeInterceptor.ATTR_SESSION_ID));
        assertEquals(TOKEN, responseHeaders.getFirst(SUB_PROTOCOL));
    }

    private ServerHttpRequest request(String subProtocol) {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        if (subProtocol != null) {
            headers.set(SUB_PROTOCOL, subProtocol);
        }
        when(request.getHeaders()).thenReturn(headers);
        when(request.getURI())
                .thenReturn(URI.create("/ws/v1/interview-sessions/" + SESSION_ID));
        return request;
    }

    private ServerHttpResponse mockResponse() {
        return mock(ServerHttpResponse.class);
    }

    private InterviewConnectionContext context(Long sessionId) {
        return InterviewConnectionContext.builder()
                .userId(5001L)
                .sessionId(sessionId)
                .scheduleId(37001L)
                .enterpriseId(10L)
                .role("CANDIDATE")
                .userType("CANDIDATE")
                .build();
    }
}
