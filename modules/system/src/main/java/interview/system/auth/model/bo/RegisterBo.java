package interview.system.auth.model.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "注册BO")
public record RegisterBo(
        @Schema(description = "用户ID")
        Long userId,

        @Schema(description = "用户名")
        String username,

        @Schema(description = "短时 JWT")
        String accessToken,

        @Schema(description = "长时 Refresh Token")
        String refreshToken,

        @Schema(description = "短时Token过期时间（秒）")
        Long expiresIn
) {
}
