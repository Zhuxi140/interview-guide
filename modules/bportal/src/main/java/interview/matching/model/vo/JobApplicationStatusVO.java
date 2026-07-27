package interview.matching.model.vo;

import interview.matching.model.enums.JobApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * 投递状态变更结果。
 */
@Schema(description = "投递状态变更结果")
public record JobApplicationStatusVO(
        @Schema(description = "投递 ID")
        Long id,
        @Schema(description = "变更后的状态")
        JobApplicationStatus status,
        @Schema(description = "状态更新时间")
        OffsetDateTime updatedAt
) {
}
