package interview.system.auth.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * @author zhuxi
 * @apiNote 登录响应
 * @since 2026/5/27 14:05
 */
@Builder
@Schema(description = "登录响应")
public record LoginVO (

    @Schema(description = "用户ID")
    Long userId,

    @Schema(description = "用户名")
    String username,

    @Schema(description = "用户昵称")
    String nickname,

    @Schema(description = "头像 URL")
    String avatarUrl,

    @Schema(description = "用户类型：HR / CANDIDATE")
    String userType,

    @Schema(description = "短时 JWT")
    String accessToken,

    @Schema(description = "长时 Refresh Token")
    String refreshToken,

    @Schema(description = "过期时间（秒）")
    Long expiresIn
) {}
