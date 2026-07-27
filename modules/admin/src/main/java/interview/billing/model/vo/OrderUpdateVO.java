package interview.billing.model.vo;

import interview.billing.model.enums.PaymentOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "支付订单更新/取消通用响应")
public record OrderUpdateVO(
        @Schema(description = "订单 ID")
        Long id,

        @Schema(description = "订单状态")
        PaymentOrderStatus status,

        @Schema(description = "更新时间")
        OffsetDateTime updatedAt
) {
}
