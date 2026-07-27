package interview.billing.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
@Schema(description = "算力套餐详情")
public record SkuDetailVO(
        @Schema(description = "SKU ID")
        Long id,

        @Schema(description = "套餐名称")
        String packageName,

        @Schema(description = "套餐详细说明")
        String description,

        @Schema(description = "售卖价格（元）")
        BigDecimal price,

        @Schema(description = "币种")
        String currency,

        @Schema(description = "包含的算力额度")
        Long tokensIncluded,

        @Schema(description = "是否上架售卖")
        Boolean isActive,

        @Schema(description = "版本号")
        Integer version,

        @Schema(description = "创建时间")
        OffsetDateTime createdAt,

        @Schema(description = "更新时间")
        OffsetDateTime updatedAt
) {
}
