package interview.system.rbac.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @since 2026-05-26
 * @apiNote 角色和用户关系表
 */

@TableName("sys_user_roles")
@Data
public class SysUserRole implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    // 必填
    private Long userId;

    // 必填
    private Long roleId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
