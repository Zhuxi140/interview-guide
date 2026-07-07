package interview.framework.security.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import interview.common.constant.AuthKeyConstant;
import interview.common.constant.SecureActionContext;
import interview.common.enums.ErrorCode;
import interview.common.enums.SmsType;
import interview.common.exception.BusinessException;
import interview.framework.annonate.RequireSecure;
import interview.framework.context.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;


/**
 * @author zhuxi
 */
@RequiredArgsConstructor
public class SecureActionInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        // 验证是否使用了@RequireSecure注解
        RequireSecure annotation = handlerMethod.getMethodAnnotation(RequireSecure.class);
        if (annotation == null) {
            // 没有使用注解，直接放行
            return true;
        }

        String token = request.getHeader("X-Secure-Action-Token");
        if (StrUtil.isBlank(token)) {
            throw new BusinessException(ErrorCode.NO_VERIFY_TOKEN);
        }

        String smsSensitiveActionTokenKey = AuthKeyConstant.getSmsSensitiveActionTokenKey(token);
        String json = stringRedisTemplate.opsForValue().get(smsSensitiveActionTokenKey);
        if (StrUtil.isBlank(json)) {
            throw new BusinessException(ErrorCode.VERIFY_TOKEN_EXPIRE);
        }

        SecureActionContext context = JSONUtil.toBean(json, SecureActionContext.class);
        if (!context.getUserId().equals(AuthContext.getRequiredUserId())){
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }

        boolean match = false;
        for (SmsType smsType : annotation.allowList()) {
            if (smsType == context.getActionType()){
                match = true;
                break;
            }
        }
        if (!match) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }

        stringRedisTemplate.delete(smsSensitiveActionTokenKey);

        request.setAttribute("SECURE_ACTION_CONTEXT", context);
        return true;
    }
}
