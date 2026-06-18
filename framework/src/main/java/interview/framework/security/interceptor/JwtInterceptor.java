package interview.framework.security.interceptor;

import cn.hutool.core.util.StrUtil;
import interview.common.enums.RiskLevel;
import interview.common.enums.RoleScope;
import interview.common.enums.UserType;
import interview.common.exception.UnauthorizedException;
import interview.common.util.JwttUtil;
import interview.framework.context.AuthContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.List;
import java.util.Optional;


/**
 * @author zhuxi
 * @apiNote JWT 拦截器
 */
@Slf4j
@Component
@AllArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwttUtil jwttUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler){
        String token = request.getHeader("Authorization");

        if(!StrUtil.isNotBlank(token) || !token.startsWith("Bearer ")){
            log.error("从消息头Authorization未获取到token或为空");
            throw new UnauthorizedException();
        }

        token = token.substring(7);
        Claims claims = jwttUtil.parseToken(token);

        Long userId = Optional.ofNullable(claims.get("userId", Long.class))
                .filter(id -> id > 0)
                .orElseThrow(()->{
                    log.error("从token中未获取到userId");
                    return new UnauthorizedException();
                });

        String userTypeString = Optional.ofNullable(claims.get("userType", String.class))
                .filter(StrUtil::isNotBlank)
                .orElseThrow(()->{
                    log.error("从token中未获取到userType");
                    return new UnauthorizedException();
                });

        Integer riskLevel = Optional.ofNullable(claims.get("riskLevel", Integer.class))
                .filter(level -> level >= RiskLevel.NO_RISK.getCode())
                .orElseThrow(() -> {
                    log.error("从token中未获取到riskLevel");
                    return new UnauthorizedException();
                });

        Number number = claims.get("enterpriseId", Number.class);
        Long enterpriseId = number != null ? number.longValue() : null;
        String username = claims.get("username", String.class);
        String roleScopeString = claims.get("roleScope", String.class);
        List<String> roleCodes = claims.get("roleCodes", List.class);


        AuthContext.setAuthContext(new AuthContext.AuthUser(
                userId,
                UserType.valueOf(userTypeString),
                RiskLevel.codeToRiskLevel(riskLevel),
                RoleScope.valueOf(roleScopeString),
                enterpriseId,
                username,
                roleCodes
        ));

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        AuthContext.remove();
    }
}
