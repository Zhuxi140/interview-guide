package interview.billing.model.req;

import interview.billing.model.enums.PaymentOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "取消支付订单请求")
public class OrderCancelReq {

    @NotNull(message = "期望状态不能为空")
    @Schema(description = "期望当前订单状态", example = "PENDING")
    private PaymentOrderStatus expectedStatus;
}
