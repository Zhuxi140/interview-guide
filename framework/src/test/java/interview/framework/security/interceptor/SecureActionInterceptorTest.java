package interview.framework.security.interceptor;

import cn.hutool.json.JSONUtil;
import interview.common.annonate.RequireSecure;
import interview.common.constant.AuthKeyConstant;
import interview.common.constant.SecureActionContext;
import interview.common.enums.ErrorCode;
import interview.common.enums.SecureActionType;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 一次性安全操作令牌拦截器单元测试。
 */
@ExtendWith(MockitoExtension.class)
class SecureActionInterceptorTest {

    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private SecureActionInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new SecureActionInterceptor(stringRedisTemplate);
        AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(1L)
                .build());
    }

    @AfterEach
    void tearDown() {
        AuthContext.remove();
    }

    @Test
    void preHandle_shouldConsumeMatchingActionTokenOnce() throws Exception {
        String token = "challenge-1.token-1";
        String tokenKey = AuthKeyConstant.getSecureActionTokenKey(token);
        String json = JSONUtil.toJsonStr(SecureActionContext.builder()
                .userId(1L)
                .actionType(SecureActionType.UPDATE_ENTERPRISE_EMAIL)
                .resourceId(10L)
                .enterpriseId(10L)
                .build());
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(tokenKey)).thenReturn(json);
        when(stringRedisTemplate.execute(any(), anyList(), eq(json))).thenReturn(1L);
        MockHttpServletRequest request = requestWithToken(token);

        boolean allowed = interceptor.preHandle(
                request,
                new MockHttpServletResponse(),
                handler("updateEmail"));

        assertTrue(allowed);
        assertInstanceOf(
                SecureActionContext.class,
                request.getAttribute(SecureActionContext.REQUEST_ATTRIBUTE));
        verify(stringRedisTemplate).execute(any(), anyList(), eq(json));
    }

    @Test
    void preHandle_shouldRejectTokenIssuedForAnotherAction() throws Exception {
        String token = "challenge-1.token-1";
        String json = JSONUtil.toJsonStr(SecureActionContext.builder()
                .userId(1L)
                .actionType(SecureActionType.DELETE_ENTERPRISE)
                .resourceId(10L)
                .enterpriseId(10L)
                .build());
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(json);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> interceptor.preHandle(
                        requestWithToken(token),
                        new MockHttpServletResponse(),
                        handler("updateEmail")));

        assertEquals(ErrorCode.SECURE_ACTION_NOT_MATCH.getCode(), exception.getCode());
        verify(stringRedisTemplate, never()).execute(any(), anyList(), anyString());
    }

    @Test
    void preHandle_shouldRejectAlreadyConsumedToken() throws Exception {
        String token = "challenge-1.token-1";
        String json = JSONUtil.toJsonStr(SecureActionContext.builder()
                .userId(1L)
                .actionType(SecureActionType.UPDATE_ENTERPRISE_EMAIL)
                .build());
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(json);
        when(stringRedisTemplate.execute(any(), anyList(), eq(json))).thenReturn(0L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> interceptor.preHandle(
                        requestWithToken(token),
                        new MockHttpServletResponse(),
                        handler("updateEmail")));

        assertEquals(ErrorCode.VERIFY_TOKEN_EXPIRE.getCode(), exception.getCode());
    }

    private MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Secure-Action-Token", token);
        return request;
    }

    private HandlerMethod handler(String methodName) throws NoSuchMethodException {
        return new HandlerMethod(
                new SecuredController(),
                SecuredController.class.getDeclaredMethod(methodName));
    }

    private static class SecuredController {

        @RequireSecure(SecureActionType.UPDATE_ENTERPRISE_EMAIL)
        public void updateEmail() {
        }
    }
}
