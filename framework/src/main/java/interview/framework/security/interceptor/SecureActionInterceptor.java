package interview.framework.security.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import interview.common.annonate.RequireSecure;
import interview.common.constant.AuthKeyConstant;
import interview.common.constant.SecureActionContext;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.framework.redis.RedisScripts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collections;
import java.util.Objects;


/**
 * 校验并原子消费一次性安全操作令牌。
 *
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

        // 仅拦截声明了 @RequireSecure 的敏感操作接口
        RequireSecure annotation = handlerMethod.getMethodAnnotation(RequireSecure.class);
        if (annotation == null) {
            return true;
        }

        // 从请求头读取一次性安全操作令牌
        String token = request.getHeader("X-Secure-Action-Token");
        if (StrUtil.isBlank(token)) {
            throw new BusinessException(ErrorCode.NO_VERIFY_TOKEN);
        }

        // 从 Redis 读取令牌携带的安全操作上下文
        String secureActionTokenKey = AuthKeyConstant.getSecureActionTokenKey(token);
        String json = stringRedisTemplate.opsForValue().get(secureActionTokenKey);
        if (StrUtil.isBlank(json)) {
            throw new BusinessException(ErrorCode.VERIFY_TOKEN_EXPIRE);
        }

        // 安全令牌必须属于当前登录用户
        SecureActionContext context = JSONUtil.toBean(json, SecureActionContext.class);
        if (!Objects.equals(context.getUserId(), AuthContext.getRequiredUserId())) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }

        // 令牌业务动作必须与接口声明的唯一动作精确一致
        if (annotation.value() != context.getActionType()) {
            throw new BusinessException(ErrorCode.SECURE_ACTION_NOT_MATCH);
        }

        // 原子比较并删除令牌，保证令牌只能被一个请求消费一次
        Long consumed = stringRedisTemplate.execute(
                RedisScripts.CONSUME_ONCE,
                Collections.singletonList(secureActionTokenKey),
                json);
        if (consumed == null || consumed != 1L) {
            throw new BusinessException(ErrorCode.VERIFY_TOKEN_EXPIRE);
        }

        // 将已验证上下文传给 Controller 和后续业务服务
        request.setAttribute(SecureActionContext.REQUEST_ATTRIBUTE, context);
        return true;
    }
}
