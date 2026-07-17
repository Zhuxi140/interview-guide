package interview.system.tenant.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import interview.framework.security.mybatis.AesTypeHandler;
import interview.system.tenant.model.enums.EnterpriseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 企业租户主表
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "enterprises",autoResultMap = true)
public class Enterprise implements Serializable {

    @TableId(type = IdType.INPUT, value = "id")
    private Long id;

    private String name;

    private String shortName;

    private String industry;

    private String scale;

    private String contactEmail;

    @TableField(typeHandler = AesTypeHandler.class)
    private String contactPhone;

    private EnterpriseStatus status;

    private String logoUrl;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableLogic
    private Boolean isDeleted;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
