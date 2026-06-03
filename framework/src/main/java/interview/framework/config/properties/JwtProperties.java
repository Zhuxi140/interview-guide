package interview.framework.config.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author zhuxi
 * @apiNote JWT 配置属性
 * @since 2026/5/27 14:05
 */

@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    @Value("${jwt.expiration}")
    private final Long expiration;
    @Value("${jwt.secret}")
    private final String secret;
}
