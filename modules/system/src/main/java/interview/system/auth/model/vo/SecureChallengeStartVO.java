package interview.system.auth.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 安全验证 Challenge 启动结果。
 */
@Schema(description = "安全验证 Challenge 启动结果")
public record SecureChallengeStartVO(
        @Schema(description = "Challenge ID", example = "f6a8d9c0873f4bbd93d271cf88f639b1")
        String challengeId,
        @Schema(description = "脱敏后的验证手机号", example = "138****8000")
        String maskedPhone,
        @Schema(description = "验证码有效秒数", example = "300")
        long expiresInSeconds
) {
}
