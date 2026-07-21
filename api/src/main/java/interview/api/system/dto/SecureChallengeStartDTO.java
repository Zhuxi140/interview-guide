package interview.api.system.dto;

/**
 * 安全验证 Challenge 内部启动结果。
 *
 * @param challengeId Challenge ID
 * @param maskedPhone 脱敏后的验证手机号
 * @param expiresInSeconds 验证码有效秒数
 */
public record SecureChallengeStartDTO(
        String challengeId,
        String maskedPhone,
        long expiresInSeconds
) {
}
