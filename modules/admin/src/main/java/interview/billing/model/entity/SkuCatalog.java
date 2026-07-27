package interview.billing.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@TableName(value = "billing_sku_catalog")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuCatalog implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String packageName;

    private String description;

    private BigDecimal price;

    @Builder.Default
    private String currency = "CNY";

    private Long tokensIncluded;

    @Builder.Default
    private Boolean isActive = true;

    @Version
    @Builder.Default
    private Integer version = 0;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableLogic
    @Builder.Default
    private Boolean isDeleted = false;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
