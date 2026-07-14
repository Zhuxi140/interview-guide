package interview.system.rbac.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;


/**
 * @author zhuxi
 * @apiNote 系统 API 资源
 */

@TableName("sys_permissions")
@Data
public class Permission implements Serializable {

    @TableId(type = IdType.INPUT)
    private Long id;

    // 必填
    private String permCode;

    // 必填
    private String permType;

    private String apiPath;

    // 默认值 1
    private Integer status;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
