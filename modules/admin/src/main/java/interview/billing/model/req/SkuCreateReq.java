package interview.billing.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "创建算力套餐请求")
public class SkuCreateReq {

    @NotBlank(message = "套餐名称不能为空")
    @Schema(description = "套餐名称", example = "基础套餐 A")
    private String packageName;

    @Schema(description = "套餐详细说明", example = "包含 10000 tokens 的基础算力包")
    private String description;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    @Schema(description = "售卖价格（元）", example = "9.99")
    private BigDecimal price;

    @NotBlank(message = "币种不能为空")
    @Schema(description = "ISO 4217 币种代码", example = "CNY")
    private String currency;

    @NotNull(message = "算力额度不能为空")
    @DecimalMin(value = "1", message = "算力额度必须大于 0")
    @Schema(description = "包含的算力额度", example = "10000")
    private Long tokensIncluded;

    @Schema(description = "是否上架售卖", example = "true")
    private Boolean isActive;
}
