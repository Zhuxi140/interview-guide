package interview.system.auth.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * @author zhuxi
 * @apiNote 注册响应
 */
@Builder
@Schema(description = "注册响应")
public record RegisterVO (

    @Schema(description = "用户ID")
    Long userId,

    @Schema(description = "用户名")
    String username,

    @Schema(description = "短时 JWT")
    String accessToken,

    @Schema(description = "长时 Refresh Token")
    String refreshToken,

    @Schema(description = "短时Token过期时间（秒）")
    Long expiresInSeconds
) {}
