package interview.job.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * 岗位编辑结果。
 */
@Schema(description = "岗位编辑结果")
public record JobUpdateVO(
        @Schema(description = "岗位 ID")
        Long id,
        @Schema(description = "更新后的版本号")
        Integer version,
        @Schema(description = "更新时间")
        OffsetDateTime updatedAt
) {
}
