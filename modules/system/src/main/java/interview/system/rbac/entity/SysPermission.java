package interview.system.rbac.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * @author zhuxi
 * @since 2026-05-26
 * @apiNote 系统 API 资源
 */

@TableName("sys_permissions")
@Data
public class SysPermission implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String permCode;

    private String permType;

    private String apiPath;

    private Integer status;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
