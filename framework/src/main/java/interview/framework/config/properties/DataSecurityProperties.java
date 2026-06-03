package interview.framework.config.properties;


import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author zhuxi
 * @apiNote 密码加密配置
 * @since 2026/5/27 14:05
 */

@ConfigurationProperties(prefix = "security")
@Data
public class DataSecurityProperties {

    // TODO:
    @Value("${security.sm4Key}")
    private final String sm4Key;
    @Value("${security.aesKey}")
    private final String aesKey;
}
