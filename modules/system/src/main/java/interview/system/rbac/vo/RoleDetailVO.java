package interview.system.rbac.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author zhuxi
 * @apiNote 角色详情响应
 * @since 2026/5/27 14:05
 */
@Builder
public record RoleDetailVO(
    Long id,
    String roleCode,
    String roleName,
    String roleScope,
    List<String> permissions,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createdAt
) {}
