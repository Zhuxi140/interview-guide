package interview.billing.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "更新算力套餐响应")
public record SkuUpdateVO(
        @Schema(description = "SKU ID")
        Long id,

        @Schema(description = "版本号")
        Integer version,

        @Schema(description = "更新时间")
        OffsetDateTime updatedAt
) {
}
