package interview.system.tenant.model.req;

import interview.framework.annonate.ValidRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 邀请成员请求
 */

@Data
@Schema(description = "邀请成员请求")
public class TeamMemberCreateReq {

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID")
    private Long userId;

    @NotNull(message = "角色ID不能为空")
    @ValidRole
    @Schema(description = "角色ID：必须匹配有效的角色枚举编码")
    private Integer roleId;
}
