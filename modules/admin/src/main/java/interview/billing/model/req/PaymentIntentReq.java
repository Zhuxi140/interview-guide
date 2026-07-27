package interview.billing.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "创建支付意图请求")
public class PaymentIntentReq {

    @NotBlank(message = "支付渠道不能为空")
    @Schema(description = "支付渠道编码", example = "alipay")
    private String provider;
}
