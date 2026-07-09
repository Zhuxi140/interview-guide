package interview.system.tenant.model.req;

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
    @Schema(description = "角色ID")
    private Integer roleId;
}
