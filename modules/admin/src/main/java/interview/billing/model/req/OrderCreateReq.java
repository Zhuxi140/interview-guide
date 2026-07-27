package interview.billing.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "创建支付订单请求")
public class OrderCreateReq {

    @NotNull(message = "SKU ID 不能为空")
    @Schema(description = "套餐 SKU ID", example = "1001")
    private Long skuId;
}
