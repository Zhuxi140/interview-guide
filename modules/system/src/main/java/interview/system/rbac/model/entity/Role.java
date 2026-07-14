package interview.system.rbac.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import interview.common.enums.RoleScope;
import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 系统角色（PLATFORM / ENTERPRISE / USER）
 */

@TableName("sys_roles")
@Data
public class Role implements Serializable {

    @TableId(type = IdType.INPUT)
    private Integer id;

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
