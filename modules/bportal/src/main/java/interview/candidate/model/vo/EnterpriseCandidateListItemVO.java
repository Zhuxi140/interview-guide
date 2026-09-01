package interview.candidate.model.vo;

import interview.candidate.model.enums.EnterpriseCandidateSource;
import interview.candidate.model.enums.EnterpriseCandidateStatus;
import interview.common.enums.AiTaskStatus;
import interview.matching.model.enums.JobApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "企业人才池列表项")
public record EnterpriseCandidateListItemVO(
        @Schema(description = "人才池候选人 ID") Long candidateId,
        @Schema(description = "候选人姓名") String candidateName,
        @Schema(description = "候选人来源") EnterpriseCandidateSource source,
        @Schema(description = "人才池状态") EnterpriseCandidateStatus status,
        @Schema(description = "最近一次投递状态") JobApplicationStatus latestApplicationStatus,
        @Schema(description = "最近一次人才画像状态") AiTaskStatus profileStatus,
        @Schema(description = "更新时间") OffsetDateTime updatedAt
) {
}
