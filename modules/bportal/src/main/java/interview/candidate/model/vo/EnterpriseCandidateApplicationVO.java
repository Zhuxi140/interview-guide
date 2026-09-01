package interview.candidate.model.vo;

import interview.matching.model.enums.JobApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "人才池候选人投递摘要")
public record EnterpriseCandidateApplicationVO(
        @Schema(description = "投递 ID") Long applicationId,
        @Schema(description = "岗位 ID") Long jobId,
        @Schema(description = "投递状态") JobApplicationStatus status,
        @Schema(description = "投递时间") OffsetDateTime createdAt
) {
}
