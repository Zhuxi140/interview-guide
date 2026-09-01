package interview.candidate.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "候选人资料更新结果")
public record CandidateProfileUpdateVO(
        @Schema(description = "用户 ID")
        Long userId,
        @Schema(description = "更新后的版本")
        Integer version,
        @Schema(description = "更新时间")
        OffsetDateTime updatedAt
) {
}
