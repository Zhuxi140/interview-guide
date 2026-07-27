package interview.billing.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "模拟支付请求（仅 dev/test 环境）")
public class SimulatePaymentReq {

    @NotBlank(message = "渠道流水号不能为空")
    @Schema(description = "模拟的渠道成功支付流水号", example = "mock_txn_001")
    private String externalTransactionId;
}
