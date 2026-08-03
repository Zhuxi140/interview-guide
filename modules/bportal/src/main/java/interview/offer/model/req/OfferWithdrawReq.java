package interview.offer.model.req;

import interview.offer.model.enums.OfferStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "撤回 Offer 请求")
public record OfferWithdrawReq(
        @NotNull(message = "期望状态不能为空")
        @Schema(description = "客户端读取到的Offer状态", example = "SENT")
        OfferStatus expectedStatus,

        @NotNull(message = "乐观锁版本号不能为空")
        @Min(value = 0, message = "乐观锁版本号不能小于0")
        @Schema(description = "客户端读取到的版本号", example = "1")
        Integer expectedVersion,

        @NotBlank(message = "撤回原因不能为空")
        @Size(max = 256, message = "撤回原因长度不能超过256个字符")
        @Schema(description = "撤回原因", example = "招聘计划调整")
        String reason
) {
}
