package interview.system.rbac.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * @author zhuxi
 * @apiNote 用户角色列表项响应
 */
@Builder
@Schema(description = "用户角色列表项响应")
public record UserRoleItemVO(
    @Schema(description = "关联ID")
    Long id,
    @Schema(description = "角色编码")
    String roleCode,
    @Schema(description = "角色名称")
    String roleName,
    @Schema(description = "角色域：PLATFORM / ENTERPRISE / USER")
    String roleScope
) {}
