package interview.system.rbac.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author zhuxi
 * @apiNote 角色详情响应
 */
@Builder
@Schema(description = "角色详情响应")
public record RoleDetailVO(
    @Schema(description = "角色ID")
    Long id,
    @Schema(description = "角色编码")
    String roleCode,
    @Schema(description = "角色名称")
    String roleName,
    @Schema(description = "角色域：PLATFORM / ENTERPRISE / USER")
    String roleScope,
    @Schema(description = "权限标识符集合")
    List<String> permissions,
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    OffsetDateTime createdAt
) {}
