package interview.system.tenant.model.vo;

/**
 * @author zhuxi
 * @apiNote 修改成员角色响应
 */
public record TeamMemberUpdateVO(
    Long id,
    Long userId,
    Long roleId
) {}
