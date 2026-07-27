package interview.billing.model.vo;

import interview.billing.model.enums.PaymentOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
@Schema(description = "创建支付订单响应")
public record OrderCreateVO(
        @Schema(description = "订单 ID")
        Long id,

        @Schema(description = "商户订单号")
        String orderNo,

        @Schema(description = "套餐快照")
        SkuSnapshotVO skuSnapshot,

        @Schema(description = "实际支付金额（元）")
        BigDecimal amount,

        @Schema(description = "币种")
        String currency,

        @Schema(description = "订单状态")
        PaymentOrderStatus status,

        @Schema(description = "订单过期时间")
        OffsetDateTime expireTime,

        @Schema(description = "创建时间")
        OffsetDateTime createdAt
) {
}
