package interview.billing.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "支付意图响应")
public record PaymentIntentVO(
        @Schema(description = "订单 ID")
        Long orderId,

        @Schema(description = "支付渠道编码")
        String provider,

        @Schema(description = "渠道支付意图/预支付 ID")
        String providerPaymentId,

        @Schema(description = "支付链接（渠道提供）")
        String paymentUrl,

        @Schema(description = "客户端支付参数（渠道提供）")
        String clientPayload,

        @Schema(description = "支付意图过期时间")
        OffsetDateTime expiresAt
) {
}
