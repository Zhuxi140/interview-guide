package interview.system.auth.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 一次性安全操作令牌签发结果。
 */
@Schema(description = "一次性安全操作令牌签发结果")
public record SecureActionTokenVO(
        @Schema(description = "一次性安全操作令牌", example = "challengeId.9da36cc67cf54ff49a4fe22622d7afd8")
        String secureActionToken,
        @Schema(description = "令牌有效秒数", example = "300")
        long expiresInSeconds
) {
}
