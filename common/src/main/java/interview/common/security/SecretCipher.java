package interview.common.security;

/**
 * 业务密钥可逆加密接口。
 */
public interface SecretCipher {

    /**
     * 加密业务密钥。
     * @param plaintext 明文密钥
     * @return 包含算法、密钥版本、随机 IV 和认证标签的密文信封
     */
    String encrypt(String plaintext);

    /**
     * 解密业务密钥。
     * @param ciphertext 密文信封
     * @return 明文密钥
     */
    String decrypt(String ciphertext);
}
