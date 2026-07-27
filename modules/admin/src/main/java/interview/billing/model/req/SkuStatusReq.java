package interview.billing.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "上架/下架套餐请求")
public class SkuStatusReq {

    @NotNull(message = "上架状态不能为空")
    @Schema(description = "是否上架", example = "true")
    private Boolean isActive;

    @NotNull(message = "乐观锁版本号不能为空")
    @Schema(description = "当前版本号", example = "0")
    private Integer expectedVersion;
}
