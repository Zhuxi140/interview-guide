package interview.system.tenant.aspect;

import interview.api.system.EnterpriseValidationApi;
import interview.common.annonate.RequireActiveEnterprise;
import interview.common.exception.AccessDeniedException;
import interview.framework.context.AuthContext;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 正式企业业务接口的经营状态拦截器。
 */
@Aspect
@Component
@Order(200)
@RequiredArgsConstructor
public class EnterpriseStatusAspect {

    private final EnterpriseValidationApi enterpriseValidationApi;

    @Around("@annotation(interview.common.annonate.RequireActiveEnterprise)"
            + " || @within(interview.common.annonate.RequireActiveEnterprise)")
    public Object validate(ProceedingJoinPoint joinPoint) throws Throwable {
        // 优先读取接口路径中的 enterpriseId，没有时使用当前企业上下文。
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] arguments = joinPoint.getArgs();
        Long enterpriseId = null;
        for (int i = 0; i < parameterNames.length; i++) {
            if ("enterpriseId".equals(parameterNames[i]) && arguments[i] instanceof Long id) {
                enterpriseId = id;
                break;
            }
        }
        if (enterpriseId == null) {
            enterpriseId = AuthContext.getEnterpriseId();
        }
        if (enterpriseId == null) {
            throw new AccessDeniedException();
        }

        enterpriseValidationApi.validateActiveEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());
        return joinPoint.proceed();
    }
}
