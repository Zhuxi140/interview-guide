package interview.system.rbac.model.vo;

import interview.framework.security.Desensitize.Desensitize;
import interview.framework.security.Desensitize.DesensitizeType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author zhuxi
 * @apiNote 平台用户详情响应
 */
@Builder
@Schema(description = "平台用户详情响应")
public record AdminUserDetailVO(

    @Schema(description = "用户ID")
    Long userId,

    @Schema(description = "登录账号")
    String username,

    @Schema(description = "邮箱")
    String email,

    @Schema(description = "用户昵称")
    String nickname,

    @Schema(description = "手机号（脱敏）")
    @Desensitize(type = DesensitizeType.PHONE)
    String phone,

    @Schema(description = "用户类型", example = "CANDIDATE",
            allowableValues = {"ENTERPRISE_USER", "CANDIDATE", "PLATFORM_ADMIN", "PLATFORM_OPS"})
    String userType,

    @Schema(description = "用户状态", example = "NORMAL", allowableValues = {"NORMAL", "DISABLED"})
    String status,

    @Schema(description = "风控等级", example = "NO_RISK",
            allowableValues = {"NO_RISK", "LOW_RISK", "MID_RISK", "HIGH_RISK"})
    String riskLevel,

    @Schema(description = "平台角色列表；无平台角色时返回空数组")
    List<UserRoleItemVO> platformRoles,

    @Schema(description = "注册时间")
    OffsetDateTime createdAt
) {}
