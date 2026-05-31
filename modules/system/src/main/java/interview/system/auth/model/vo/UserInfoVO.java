package interview.system.auth.model.vo;

import interview.framework.security.Desensitize.Desensitize;
import interview.framework.security.Desensitize.DesensitizeType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * @author zhuxi
 * @apiNote 当前用户信息响应
 * @since 2026/5/27 14:05
 */
@Builder
@Schema(description = "当前用户信息响应")
public record UserInfoVO (

    @Schema(description = "用户ID")
    Long id,

    @Schema(description = "登录账号")
    String username,

    @Schema(description = "邮箱")
    String email,

    @Schema(description = "用户昵称")
    String nickname,

    @Schema(description = "头像 URL")
    String avatarUrl,

    @Schema(description = "手机号")
    @Desensitize(type = DesensitizeType.PHONE)
    String phone,

    @Schema(description = "用户类型：HR / CANDIDATE / PLATFORM_ADMIN")
    String userType,

    @Schema(description = "状态：1-正常 0-禁用")
    Integer status
) {}
