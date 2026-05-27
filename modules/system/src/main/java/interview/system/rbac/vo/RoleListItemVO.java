package interview.system.rbac.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * @author zhuxi
 * @apiNote 角色列表项响应
 * @since 2026/5/27 14:05
 */
@Builder
public record RoleListItemVO(
    Long id,
    String roleCode,
    String roleName,
    String roleScope,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createdAt
) {}
