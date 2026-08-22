package interview.system.auth.model.vo;

import interview.common.enums.UserType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

/**
 * @author zhuxi
 * @apiNote 登录响应
 */
@Builder
@Schema(description = "登录响应")
public record LoginVO (

    @Schema(description = "用户ID")
    Long userId,

    @Schema(description = "当前企业 ID；登录后尚未选择工作区，固定为空")
    Long enterpriseId,

    @Schema(description = "当前企业名称；登录后尚未选择工作区，固定为空")
    String enterpriseName,

    @Schema(description = "当前企业 Logo URL；登录后尚未选择工作区，固定为空")
    String logoUrl,

    @Schema(description = "用户所属企业列表")
    List<UserEnterpriseVO> enterprises,

    @Schema(description = "用户名")
    String username,

    @Schema(description = "用户昵称")
    String nickname,

    @Schema(description = "头像 URL")
    String avatarUrl,

    @Schema(description = "用户类型", example = "CANDIDATE",
            allowableValues = {"ENTERPRISE_USER", "CANDIDATE", "PLATFORM_ADMIN", "PLATFORM_OPS"})
    UserType userType,

    @Schema(description = "角色编码集合")
    List<String> roles,

    @Schema(description = "权限标识符集合")
    List<String> permissions,

    @Schema(description = "短时 JWT")
    String accessToken,

    @Schema(description = "长时 Refresh Token")
    String refreshToken,

    @Schema(description = "短时token过期时间（秒）")
    Long expiresInSeconds
) {}
