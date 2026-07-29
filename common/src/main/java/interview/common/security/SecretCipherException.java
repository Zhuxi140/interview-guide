package interview.common.security;

/**
 * 业务密钥配置、加密或解密失败异常。
 */
public class SecretCipherException extends RuntimeException {

    public SecretCipherException(String message) {
        super(message);
    }

    public SecretCipherException(String message, Throwable cause) {
        super(message, cause);
    }
}
