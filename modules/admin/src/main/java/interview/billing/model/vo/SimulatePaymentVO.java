package interview.billing.model.vo;

import interview.billing.model.enums.PaymentOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "模拟支付响应")
public record SimulatePaymentVO(
        @Schema(description = "订单 ID")
        Long id,

        @Schema(description = "商户订单号")
        String orderNo,

        @Schema(description = "订单状态")
        PaymentOrderStatus status,

        @Schema(description = "充值算力额度")
        Long tokensGranted
) {
}
