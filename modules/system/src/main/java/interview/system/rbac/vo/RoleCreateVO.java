package interview.system.rbac.vo;

import lombok.Builder;

/**
 * @author zhuxi
 * @apiNote 创建角色响应
 * @since 2026/5/27 14:05
 */
@Builder
public record RoleCreateVO(
    Long id,
    String roleCode,
    String roleName,
    String roleScope
) {}
