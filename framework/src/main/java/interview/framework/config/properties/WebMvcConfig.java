package interview.framework.config.properties;

import interview.common.util.JwttUtil;
import interview.framework.security.interceptor.JwtInterceptor;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author zhuxi
 * @apiNote WebMvc配置
 * @since 2026/5/27 14:05
 */


@Configuration
@AllArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwttUtil jwttUtil;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(new JwtInterceptor(jwttUtil))
                .addPathPatterns("/**")
                .excludePathPatterns("/api/v1/auth/**");
    }
}
