package interview.system.tenant.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author zhuxi
 * @since 2026-05-26
 * @apiNote 企业租户主表
 */

@Data
@TableName(value = "enterprises",autoResultMap = true)
public class Enterprise implements Serializable {

    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;

    private String name;

    private String shortName;

    private String industry;

    private String scale;

    private String contactEmail;

    private String contactPhone;

    private Integer status;

    private String logoUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Boolean isDeleted;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
