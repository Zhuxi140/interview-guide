package interview.framework.security.interceptor;

import interview.common.enums.ErrorCode;
import interview.common.enums.RiskLevel;
import interview.common.exception.BusinessException;
import interview.framework.annonate.MaxRiskLevel;
import interview.framework.context.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @author zhuxi
 */


public class RiskCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod handlerMethod)){
            return true;
        }

        MaxRiskLevel methodAnnotation = handlerMethod.getMethodAnnotation(MaxRiskLevel.class);
        if (methodAnnotation == null){
            return true;
        }

        RiskLevel level = methodAnnotation.value();
        RiskLevel currentLevel = AuthContext.getRequiredRiskLevel();
        if (currentLevel.getCode() > level.getCode()){
            throw new BusinessException(ErrorCode.RISK_CONTROL);
        }

        return true;
    }
}
