package interview.framework.config;

import interview.common.util.JwttUtil;
import interview.framework.security.interceptor.JwtInterceptor;
import interview.framework.security.interceptor.RiskCheckInterceptor;
import interview.framework.security.interceptor.SecureActionInterceptor;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author zhuxi
 * @apiNote WebMvc配置
 */


@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwttUtil jwttUtil;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(new JwtInterceptor(jwttUtil, stringRedisTemplate))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/api/v1/auth/login/sms",
                        "/api/v1/auth/password/reset",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/send-sms",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/api-docs/**"
                        )
                        .order(1);

        registry.addInterceptor(new RiskCheckInterceptor())
                .excludePathPatterns(
                        "/api/v1/auth/register",
                        "/api/v1/auth/send-sms",
                        "/api/v1/auth/login",
                        "/api/v1/auth/login/sms",
                        "/api/v1/auth/password/reset",
                        "/api/v1/auth/refresh",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/api-docs/**"
                )
                .order(2);

        registry.addInterceptor(new SecureActionInterceptor(stringRedisTemplate))
                .excludePathPatterns(
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/api/v1/auth/login/sms",
                        "/api/v1/auth/password/reset",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/send-sms",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/api-docs/**"
                )
                .order(3);
    }


}
