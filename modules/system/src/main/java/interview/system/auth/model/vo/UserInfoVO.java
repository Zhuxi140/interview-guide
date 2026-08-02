package interview.system.auth.model.vo;

import interview.framework.security.Desensitize.Desensitize;
import interview.framework.security.Desensitize.DesensitizeType;
import interview.system.auth.model.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

/**
 * @author zhuxi
 * @apiNote 当前用户信息响应
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

    @Schema(description = "用户类型", example = "CANDIDATE",
            allowableValues = {"ENTERPRISE_USER", "CANDIDATE", "PLATFORM_ADMIN", "PLATFORM_OPS"})
    String userType,

    @Schema(description = "当前 JWT 选中的企业 ID；未选择企业时为空")
    Long enterpriseId,

    @Schema(description = "当前企业名称")
    String enterpriseName,

    @Schema(description = "当前企业 Logo URL")
    String logoUrl,

    @Schema(description = "用户所属企业列表；无所属企业时返回空数组")
    List<UserEnterpriseVO> enterprises,

    @Schema(description = "用户状态", example = "NORMAL", allowableValues = {"NORMAL", "DISABLED"})
    UserStatus status,

    @Schema(description = "角色编码集合")
    List<String> roles,

    @Schema(description = "权限标识符集合")
    List<String> permissions
) {}
