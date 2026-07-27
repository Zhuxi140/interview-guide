package interview.billing.model.vo;

import interview.billing.model.enums.PaymentOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
@Schema(description = "支付订单列表项（企业端）")
public record OrderListItemVO(
        @Schema(description = "订单 ID")
        Long id,

        @Schema(description = "商户订单号")
        String orderNo,

        @Schema(description = "实际支付金额（元）")
        BigDecimal amount,

        @Schema(description = "币种")
        String currency,

        @Schema(description = "充值算力额度")
        Long tokensGranted,

        @Schema(description = "订单状态")
        PaymentOrderStatus status,

        @Schema(description = "订单过期时间")
        OffsetDateTime expireTime,

        @Schema(description = "创建时间")
        OffsetDateTime createdAt
) {
}
