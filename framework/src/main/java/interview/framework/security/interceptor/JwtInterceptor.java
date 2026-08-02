package interview.framework.security.interceptor;

import cn.hutool.core.util.StrUtil;
import interview.common.constant.AuthKeyConstant;
import interview.common.enums.RiskLevel;
import interview.common.enums.Role;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler){
        String token = request.getHeader("Authorization");

        if(!StrUtil.isNotBlank(token) || !token.startsWith("Bearer ")){
            log.error("从消息头Authorization未获取到token或为空");
            throw new UnauthorizedException();
        }

        token = token.substring(7);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(AuthKeyConstant.getTokenBanKey(token)))) {
            throw new UnauthorizedException();
        }
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

        // 解析平台角色
        List<String> stringPlatformRoleCodes = claims.get("platformRoleCodes", List.class);
        List<Role> platformRoleCodes = stringPlatformRoleCodes != null
                ? stringPlatformRoleCodes.stream()
                    .map(Role::fromString)
                    .toList()
                : Collections.emptyList();

        // 解析企业角色映射
        Map<Long, List<Role>> entRoleMap = new HashMap<>();
        Object rawEntRoleMap = claims.get("entRoleMap", Map.class);
        if (rawEntRoleMap instanceof Map<?,?> rawMap) {
            for (Map.Entry<?,?> entry : rawMap.entrySet()) {
                Long entId = Long.valueOf(entry.getKey().toString());
                List<Role> roles = ((List<?>)entry.getValue()).stream()
                        .map(o -> Role.fromString(o.toString()))
                        .toList();
                entRoleMap.put(entId, roles);
            }
        }

        AuthContext.setAuthContext(new AuthContext.AuthUser(
                userId,
                UserType.valueOf(userTypeString),
                RiskLevel.codeToRiskLevel(riskLevel),
                enterpriseId,
                username,
                platformRoleCodes,
                entRoleMap
        ));

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        AuthContext.remove();
    }
}
