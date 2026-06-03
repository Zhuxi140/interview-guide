package interview.system.rbac.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import interview.common.enums.RoleScope;
import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @since 2026-05-26
 * @apiNote 系统角色（PLATFORM / ENTERPRISE / USER）
 */

@TableName("sys_roles")
@Data
public class SysRole implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    // 必填
    private String roleCode;

    // 必填
    private String roleName;

    // 必填
    private RoleScope roleScope;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
