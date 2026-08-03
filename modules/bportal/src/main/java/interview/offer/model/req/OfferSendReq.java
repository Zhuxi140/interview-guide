package interview.offer.model.req;

import interview.offer.model.enums.OfferStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "发送 Offer 请求")
public record OfferSendReq(
        @NotNull(message = "期望状态不能为空")
        @Schema(description = "客户端读取到的Offer状态", example = "DRAFT")
        OfferStatus expectedStatus,

        @NotNull(message = "乐观锁版本号不能为空")
        @Min(value = 0, message = "乐观锁版本号不能小于0")
        @Schema(description = "客户端读取到的版本号", example = "0")
        Integer expectedVersion
) {
}
