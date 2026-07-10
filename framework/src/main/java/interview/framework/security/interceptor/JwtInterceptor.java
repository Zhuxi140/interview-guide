package interview.framework.security.interceptor;

import cn.hutool.core.util.StrUtil;
import interview.common.enums.RiskLevel;
import interview.common.enums.Role;
import interview.common.enums.RoleScope;
import interview.common.enums.UserType;
import interview.common.exception.UnauthorizedException;
import interview.common.util.JwttUtil;
import interview.framework.context.AuthContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * @author zhuxi
 * @apiNote JWT 拦截器
 */
@Slf4j
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwttUtil jwttUtil;

    @Override
    public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler){
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
        List<String> roleScopeStrings = claims.get("roleScopes", List.class);
        List<RoleScope> roleScopes = roleScopeStrings != null
                ? roleScopeStrings.stream()
                .map(RoleScope::getRoleScope)
                .toList()
                : Collections.emptyList();
        List<String> stringRoleCodes = claims.get("roleCodes", List.class);
        List<Role> roleCodes = stringRoleCodes != null
                ? stringRoleCodes.stream()
                    .map(Role::fromString)
                    .collect(Collectors.toList())
                : Collections.emptyList();


        AuthContext.setAuthContext(new AuthContext.AuthUser(
                userId,
                UserType.valueOf(userTypeString),
                RiskLevel.codeToRiskLevel(riskLevel),
                roleScopes,
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
