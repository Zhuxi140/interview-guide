package interview.api.system.dto;

/**
 * 跨模块用户基础资料更新命令；null 表示不更新对应字段。
 */
public record UserProfileUpdateDTO(
        String displayName,
        String email
) {
}
