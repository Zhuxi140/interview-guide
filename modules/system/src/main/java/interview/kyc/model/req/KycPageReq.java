package interview.kyc.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 平台端审核接口通用分页参数。
 */
@Data
@Schema(description = "平台端审核接口通用分页参数")
public class KycPageReq {

    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码从 1 开始")
    @Schema(description = "页码（从 1 开始）", example = "1")
    private Integer page;

    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数至少为 1")
    @Max(value = 100, message = "每页条数不能超过 100")
    @Schema(description = "每页条数（1~100）", example = "20")
    private Integer size;
}
