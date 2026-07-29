package interview.framework.security.crypto;

import interview.common.security.SecretCipherException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AesGcmSecretCipherTest {

    private static final String KEY_V1 = "00112233445566778899aabbccddeeff";
    private static final String KEY_V2 = "ffeeddccbbaa99887766554433221100";

    @Test
    void encryptAndDecrypt_shouldRoundTrip() {
        // 使用当前激活密钥生成带版本的 AES-GCM 密文信封。
        AesGcmSecretCipher cipher = new AesGcmSecretCipher(
                "v1",
                Map.of("v1", KEY_V1)
        );

        String encrypted = cipher.encrypt("sk-test-secret");

        assertTrue(encrypted.startsWith("v1:aes-gcm:v1:"));
        assertEquals("sk-test-secret", cipher.decrypt(encrypted));
    }

    @Test
    void encrypt_shouldUseIndependentRandomIv() {
        // 相同明文重复加密时应产生不同密文。
        AesGcmSecretCipher cipher = new AesGcmSecretCipher(
                "v1",
                Map.of("v1", KEY_V1)
        );

        String first = cipher.encrypt("same-secret");
        String second = cipher.encrypt("same-secret");

        assertNotEquals(first, second);
        assertEquals("same-secret", cipher.decrypt(first));
        assertEquals("same-secret", cipher.decrypt(second));
    }

    @Test
    void decrypt_shouldRejectTamperedCiphertext() {
        // 修改密文载荷后，GCM 认证标签必须阻止解密。
        AesGcmSecretCipher cipher = new AesGcmSecretCipher(
                "v1",
                Map.of("v1", KEY_V1)
        );
        String encrypted = cipher.encrypt("sk-test-secret");
        int payloadStart = encrypted.lastIndexOf(':') + 1;
        char replacement = encrypted.charAt(payloadStart) == 'A' ? 'B' : 'A';
        String tampered = encrypted.substring(0, payloadStart)
                + replacement
                + encrypted.substring(payloadStart + 1);

        assertThrows(SecretCipherException.class, () -> cipher.decrypt(tampered));
    }

    @Test
    void decrypt_shouldSupportHistoricalKeyAfterRotation() {
        // 新组件切换到 v2 后，保留 v1 即可继续解密历史密文。
        AesGcmSecretCipher oldCipher = new AesGcmSecretCipher(
                "v1",
                Map.of("v1", KEY_V1)
        );
        String historicalCiphertext = oldCipher.encrypt("historical-secret");

        AesGcmSecretCipher rotatedCipher = new AesGcmSecretCipher(
                "v2",
                Map.of("v1", KEY_V1, "v2", KEY_V2)
        );

        assertEquals("historical-secret", rotatedCipher.decrypt(historicalCiphertext));
        assertTrue(rotatedCipher.encrypt("new-secret").startsWith("v1:aes-gcm:v2:"));
    }

    @Test
    void constructor_shouldRejectMissingActiveKey() {
        // 激活 ID 没有对应密钥时应在启动阶段立即失败。
        assertThrows(
                SecretCipherException.class,
                () -> new AesGcmSecretCipher("v2", Map.of("v1", KEY_V1))
        );
    }
}
