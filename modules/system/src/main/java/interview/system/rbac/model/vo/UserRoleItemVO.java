package interview.system.rbac.model.vo;

import lombok.Builder;

/**
 * @author zhuxi
 * @apiNote 用户角色列表项响应
 */
@Builder
public record UserRoleItemVO(
    Long id,
    String roleCode,
    String roleName,
    String roleScope
) {}
