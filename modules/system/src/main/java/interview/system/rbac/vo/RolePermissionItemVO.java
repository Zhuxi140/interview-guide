package interview.system.rbac.vo;

import lombok.Builder;

/**
 * @author zhuxi
 * @apiNote 角色已拥有的权限列表项响应
 * @since 2026/5/27 14:05
 */
@Builder
public record RolePermissionItemVO(
    Long id,
    String permCode,
    String permType,
    String apiPath
) {}
