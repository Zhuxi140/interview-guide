package interview.system.auth.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * @author zhuxi
 * @apiNote 刷新 Token 响应
 * @since 2026/5/27 14:05
 */
@Builder
@Schema(description = "刷新 Token 响应")
public record RefreshTokenVO (

    @Schema(description = "新短时 JWT")
    String accessToken,

    @Schema(description = "新长时 Refresh Token")
    String refreshToken,

    @Schema(description = "过期时间（秒）")
    Long expiresIn
) {}
