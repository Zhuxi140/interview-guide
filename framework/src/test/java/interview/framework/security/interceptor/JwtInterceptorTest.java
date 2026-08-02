package interview.framework.security.interceptor;

import interview.common.constant.AuthKeyConstant;
import interview.common.exception.UnauthorizedException;
import interview.common.util.JwttUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class JwtInterceptorTest {

    @Test
    void preHandle_shouldRejectBlacklistedToken() {
        JwttUtil jwttUtil = mock(JwttUtil.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer old-token");
        when(redisTemplate.hasKey(AuthKeyConstant.getTokenBanKey("old-token"))).thenReturn(true);

        JwtInterceptor interceptor = new JwtInterceptor(jwttUtil, redisTemplate);

        assertThrows(UnauthorizedException.class,
                () -> interceptor.preHandle(request, mock(HttpServletResponse.class), new Object()));
        verifyNoInteractions(jwttUtil);
    }
}
