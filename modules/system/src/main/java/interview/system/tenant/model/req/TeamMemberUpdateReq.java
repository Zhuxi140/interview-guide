package interview.system.tenant.model.req;

import interview.framework.annonate.ValidRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 修改成员角色请求
 */

@Data
@Schema(description = "修改成员角色请求")
public class TeamMemberUpdateReq {

    @NotNull(message = "角色ID不能为空")
    @ValidRole
    @Schema(description = "角色ID：必须匹配有效的角色枚举编码")
    private Integer roleId;
}
