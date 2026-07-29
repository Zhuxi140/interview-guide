package interview.framework.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * AES-GCM 业务密钥加密配置。
 */
@Data
@ConfigurationProperties(prefix = "security.secret-cipher")
public class SecretCipherProperties {

    /**
     * 新密文使用的密钥 ID。
     */
    private String activeKeyId;

    /**
     * 密钥 ID 到十六进制 AES 密钥的映射。
     */
    private Map<String, String> keys = new HashMap<>();
}
