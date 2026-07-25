package interview.textinterview.model.vo;

import interview.common.enums.InterviewSessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "面试会话状态")
public record InterviewSessionVO(
        @Schema(description = "会话ID")
        Long id,

        @Schema(description = "排期ID")
        Long scheduleId,

        @Schema(description = "计划提问总数")
        Integer totalQuestions,

        @Schema(description = "当前进行到的题目索引")
        Integer currentQuestionIndex,

        @Schema(description = "会话状态 CREATED / IN_PROGRESS / COMPLETED")
        InterviewSessionStatus status,

        @Schema(description = "会话最后事件序号，用于识别状态是否已推进")
        Long sessionVersion,

        @Schema(description = "创建时间")
        OffsetDateTime createdAt
) {
}
