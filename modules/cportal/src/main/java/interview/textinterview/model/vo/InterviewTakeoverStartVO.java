package interview.textinterview.model.vo;

import interview.textinterview.model.enums.InterviewTakeoverStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "人工接管开始结果")
public record InterviewTakeoverStartVO(
        @Schema(description = "接管记录ID", example = "35001")
        Long takeoverId,

        @Schema(description = "面试会话ID", example = "34001")
        Long sessionId,

        @Schema(description = "接管状态", example = "ACTIVE")
        InterviewTakeoverStatus status,

        @Schema(description = "接管开始时间")
        OffsetDateTime startedAt
) {
}
