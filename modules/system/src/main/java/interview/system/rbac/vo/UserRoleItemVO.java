package interview.system.rbac.vo;

import lombok.Builder;

/**
 * @author zhuxi
 * @apiNote 用户角色列表项响应
 * @since 2026/5/27 14:05
 */
@Builder
public record UserRoleItemVO(
    Long id,
    String roleCode,
    String roleName,
    String roleScope
) {}
