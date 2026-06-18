package interview.system.rbac.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 角色列表项响应
 */
@Builder
public record RoleListItemVO(
    Long id,
    String roleCode,
    String roleName,
    String roleScope,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    OffsetDateTime createdAt
) {}
