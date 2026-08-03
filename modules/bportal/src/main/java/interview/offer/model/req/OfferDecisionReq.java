package interview.offer.model.req;

import interview.offer.model.enums.OfferDecision;
import interview.offer.model.enums.OfferStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "候选人 Offer 决策请求")
public record OfferDecisionReq(
        @NotNull(message = "期望状态不能为空")
        @Schema(description = "客户端读取到的Offer状态", example = "SENT")
        OfferStatus expectedStatus,

        @NotNull(message = "Offer决策不能为空")
        @Schema(description = "接受或拒绝Offer", example = "ACCEPT")
        OfferDecision decision,

        @Size(max = 256, message = "决策原因长度不能超过256个字符")
        @Schema(description = "可选的决策原因", example = "已接受其他机会")
        String reason
) {
}
