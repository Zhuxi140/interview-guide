package interview.framework.config;

import interview.common.util.JwttUtil;
import interview.framework.config.properties.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author zhuxi
 * @apiNote 认证配置
 * @since 2026/5/27 14:05
 */

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public JwttUtil jwttUntil(JwtProperties properties){
        return new JwttUtil(properties.getExpiration(), properties.getSecret());
    }
}
