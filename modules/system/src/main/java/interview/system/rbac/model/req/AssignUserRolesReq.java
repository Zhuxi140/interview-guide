package interview.system.rbac.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * @author zhuxi
 * @apiNote 为用户分配角色请求
 */
@Data
@Schema(description = "为用户分配角色请求")
public class AssignUserRolesReq {

    @NotEmpty(message = "角色 ID 列表不能为空")
    @Schema(description = "角色 ID 列表")
    private List<Integer> roleIds;
}
