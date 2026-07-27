package interview.job.model.vo;

import interview.job.model.enums.JobStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * 岗位状态流转结果。
 */
@Schema(description = "岗位状态流转结果")
public record JobStatusUpdateVO(
        @Schema(description = "岗位 ID")
        Long id,
        @Schema(description = "更新后的岗位状态")
        JobStatus status,
        @Schema(description = "更新后的版本号")
        Integer version,
        @Schema(description = "更新时间")
        OffsetDateTime updatedAt
) {
}
