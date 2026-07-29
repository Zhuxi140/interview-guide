package interview.framework.security.crypto;

import interview.common.security.SecretCipher;
import interview.common.security.SecretCipherException;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 带密钥版本和认证标签的 AES-GCM 业务密钥加密器。
 */
public class AesGcmSecretCipher implements SecretCipher {

    private static final String FORMAT_VERSION = "v1";
    private static final String ALGORITHM = "aes-gcm";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int TAG_LENGTH_BYTES = TAG_LENGTH_BITS / Byte.SIZE;
    private static final Pattern KEY_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,32}");
    private static final Base64.Encoder BASE64_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_DECODER = Base64.getUrlDecoder();

    private final String activeKeyId;
    private final Map<String, SecretKey> keyRing;
    private final SecureRandom secureRandom;

    public AesGcmSecretCipher(String activeKeyId, Map<String, String> hexKeys) {
        this(activeKeyId, hexKeys, new SecureRandom());
    }

    AesGcmSecretCipher(String activeKeyId,
                       Map<String, String> hexKeys,
                       SecureRandom secureRandom) {
        // 启动时完成全部密钥校验，避免运行到业务请求时才发现配置错误。
        this.activeKeyId = validateKeyId(activeKeyId);
        this.keyRing = parseKeyRing(hexKeys);
        if (!keyRing.containsKey(this.activeKeyId)) {
            throw new SecretCipherException("当前激活的业务密钥 ID 未配置");
        }
        this.secureRandom = secureRandom;
    }

    @Override
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            throw new SecretCipherException("待加密的业务密钥不能为空");
        }

        // 每次加密生成独立随机 IV，并把信封头作为 AAD 参与认证。
        byte[] iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);
        String header = buildHeader(activeKeyId);
        byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    keyRing.get(activeKeyId),
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv)
            );
            cipher.updateAAD(header.getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(plaintextBytes);
            return header
                    + ":" + BASE64_ENCODER.encodeToString(iv)
                    + ":" + BASE64_ENCODER.encodeToString(encrypted);
        } catch (GeneralSecurityException e) {
            throw new SecretCipherException("业务密钥加密失败", e);
        } finally {
            Arrays.fill(plaintextBytes, (byte) 0);
        }
    }

    @Override
    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) {
            throw new SecretCipherException("待解密的密文不能为空");
        }

        // 解析并校验密文信封，按照信封中的密钥 ID 支持历史密文解密。
        String[] parts = ciphertext.split(":", 5);
        if (parts.length != 5
                || !FORMAT_VERSION.equals(parts[0])
                || !ALGORITHM.equals(parts[1])) {
            throw new SecretCipherException("业务密钥密文格式不受支持");
        }
        String keyId = validateKeyId(parts[2]);
        SecretKey key = keyRing.get(keyId);
        if (key == null) {
            throw new SecretCipherException("业务密钥版本不可用");
        }

        try {
            byte[] iv = BASE64_DECODER.decode(parts[3]);
            byte[] encrypted = BASE64_DECODER.decode(parts[4]);
            if (iv.length != IV_LENGTH_BYTES || encrypted.length < TAG_LENGTH_BYTES) {
                throw new SecretCipherException("业务密钥密文格式不合法");
            }

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv)
            );
            cipher.updateAAD(buildHeader(keyId).getBytes(StandardCharsets.UTF_8));
            byte[] plaintextBytes = cipher.doFinal(encrypted);
            try {
                return new String(plaintextBytes, StandardCharsets.UTF_8);
            } finally {
                Arrays.fill(plaintextBytes, (byte) 0);
            }
        } catch (AEADBadTagException e) {
            throw new SecretCipherException("业务密钥密文认证失败", e);
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            throw new SecretCipherException("业务密钥解密失败", e);
        }
    }

    /**
     * 校验并转换配置中的十六进制 AES 密钥。
     */
    private Map<String, SecretKey> parseKeyRing(Map<String, String> hexKeys) {
        if (hexKeys == null || hexKeys.isEmpty()) {
            throw new SecretCipherException("至少需要配置一个业务加密密钥");
        }

        Map<String, SecretKey> parsedKeys = new HashMap<>();
        hexKeys.forEach((keyId, hexKey) -> {
            String validatedKeyId = validateKeyId(keyId);
            byte[] keyBytes;
            try {
                keyBytes = HexFormat.of().parseHex(hexKey);
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new SecretCipherException("业务加密密钥必须使用十六进制编码", e);
            }
            if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
                throw new SecretCipherException("AES 密钥长度必须为 128、192 或 256 位");
            }
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            Arrays.fill(keyBytes, (byte) 0);
            parsedKeys.put(validatedKeyId, secretKey);
        });
        return Map.copyOf(parsedKeys);
    }

    /**
     * 限制密钥 ID 字符集，保证密文信封能够稳定解析。
     */
    private String validateKeyId(String keyId) {
        if (keyId == null || !KEY_ID_PATTERN.matcher(keyId).matches()) {
            throw new SecretCipherException("业务加密密钥 ID 格式不合法");
        }
        return keyId;
    }

    private String buildHeader(String keyId) {
        return FORMAT_VERSION + ":" + ALGORITHM + ":" + keyId;
    }
}
