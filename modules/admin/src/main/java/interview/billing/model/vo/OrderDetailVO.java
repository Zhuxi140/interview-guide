package interview.billing.model.vo;

import interview.billing.model.enums.PaymentOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
@Schema(description = "支付订单详情（企业端）")
public record OrderDetailVO(
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

        @Schema(description = "充值算力额度")
        Long tokensGranted,

        @Schema(description = "订单状态")
        PaymentOrderStatus status,

        @Schema(description = "支付渠道")
        String paymentProvider,

        @Schema(description = "渠道成功支付流水号")
        String externalTransactionId,

        @Schema(description = "订单过期时间")
        OffsetDateTime expireTime,

        @Schema(description = "支付完成时间")
        OffsetDateTime paidAt,

        @Schema(description = "用户主动取消时间")
        OffsetDateTime cancelledAt,

        @Schema(description = "系统判定过期时间")
        OffsetDateTime expiredAt,

        @Schema(description = "创建时间")
        OffsetDateTime createdAt
) {
}
