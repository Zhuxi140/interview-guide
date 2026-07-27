package interview.billing.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "更新算力套餐请求")
public class SkuUpdateReq {

    @Schema(description = "套餐名称")
    private String packageName;

    @Schema(description = "套餐详细说明")
    private String description;

    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    @Schema(description = "售卖价格（元）")
    private BigDecimal price;

    @Schema(description = "ISO 4217 币种代码", example = "CNY")
    private String currency;

    @DecimalMin(value = "1", message = "算力额度必须大于 0")
    @Schema(description = "包含的算力额度")
    private Long tokensIncluded;

    @Schema(description = "是否上架售卖")
    private Boolean isActive;

    @NotNull(message = "乐观锁版本号不能为空")
    @Schema(description = "乐观锁版本号", example = "0")
    private Integer expectedVersion;
}
