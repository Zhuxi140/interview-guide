package interview.candidate.model.vo;

import interview.common.enums.InterviewScheduleStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "候选人面试轮次摘要")
public record EnterpriseCandidateInterviewRoundVO(
        @Schema(description = "排期 ID") Long scheduleId,
        @Schema(description = "投递 ID") Long applicationId,
        @Schema(description = "轮次") Short roundNo,
        @Schema(description = "阶段编码") String phaseCode,
        @Schema(description = "排期状态") InterviewScheduleStatus status,
        @Schema(description = "面试时间") OffsetDateTime interviewTime
) {
}
