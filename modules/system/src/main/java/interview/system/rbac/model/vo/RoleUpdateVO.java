package interview.system.rbac.model.vo;

/**
 * @author zhuxi
 * @apiNote 更新角色响应
 * @since 2026/5/27 14:05
 */
public record RoleUpdateVO(
    Long id,
    String roleCode,
    String roleName
) {}
