package interview.billing.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "上架/下架套餐响应")
public record SkuStatusVO(
        @Schema(description = "SKU ID")
        Long id,

        @Schema(description = "是否上架")
        Boolean isActive,

        @Schema(description = "版本号")
        Integer version,

        @Schema(description = "更新时间")
        OffsetDateTime updatedAt
) {
}
