package interview.billing.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@Schema(description = "套餐快照（订单创建/详情中嵌套使用）")
public record SkuSnapshotVO(
        @Schema(description = "套餐名称")
        String packageName,

        @Schema(description = "套餐单价（元）")
        BigDecimal unitPrice,

        @Schema(description = "币种")
        String currency,

        @Schema(description = "包含的算力额度")
        Long tokensIncluded
) {
}
