package interview.system.rbac.model.vo;

import lombok.Builder;

/**
 * @author zhuxi
 * @apiNote 权限资源列表项响应
 * @since 2026/5/27 14:05
 */
@Builder
public record PermissionItemVO(
    Long id,
    String permCode,
    String permType,
    String apiPath,
    Integer status
) {}
