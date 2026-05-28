package interview.system.tenant.model.vo;

/**
 * @author zhuxi
 * @apiNote 邀请成员响应
 * @since 2026/5/27 14:05
 */
public record TeamMemberCreateVO(
    Long id,
    Long userId,
    Long roleId
) {}
