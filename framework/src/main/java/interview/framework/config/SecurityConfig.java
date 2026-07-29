package interview.framework.config;

import interview.common.security.SecretCipher;
import interview.common.util.DataSecurityUtil;
import interview.common.util.JwttUtil;
import interview.framework.config.properties.DataSecurityProperties;
import interview.framework.config.properties.JwtProperties;
import interview.framework.config.properties.SecretCipherProperties;
import interview.framework.security.crypto.AesGcmSecretCipher;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author zhuxi
 * @apiNote 认证配置
 */

@Configuration
@EnableConfigurationProperties({
        JwtProperties.class,
        DataSecurityProperties.class,
        SecretCipherProperties.class
})
public class SecurityConfig {

    @Bean
    public JwttUtil jwttUntil(JwtProperties properties){
        return new JwttUtil(properties.getExpiration(), properties.getSecret());
    }

    @Bean
    public DataSecurityUtil dataSecurityUtil(DataSecurityProperties properties){
        return new DataSecurityUtil(properties.getSm4Key(), properties.getAesKey());
    }

    @Bean
    public SecretCipher secretCipher(SecretCipherProperties properties) {
        return new AesGcmSecretCipher(
                properties.getActiveKeyId(),
                properties.getKeys()
        );
    }
}
