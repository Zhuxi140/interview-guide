package interview.textinterview.model.vo;

import interview.common.enums.InterviewSessionStatus;
import interview.textinterview.model.enums.InterviewTakeoverStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "人工接管结束结果")
public record InterviewTakeoverEndVO(
        @Schema(description = "接管记录ID", example = "35001")
        Long takeoverId,

        @Schema(description = "接管状态", example = "ENDED")
        InterviewTakeoverStatus status,

        @Schema(description = "结束接管后的会话状态", example = "IN_PROGRESS")
        InterviewSessionStatus sessionStatus,

        @Schema(description = "接管结束时间")
        OffsetDateTime endedAt
) {
}
