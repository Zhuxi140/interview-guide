package interview.billing.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@Schema(description = "算力套餐（公开）")
public record SkuVO(
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
        Long tokensIncluded
) {
}
