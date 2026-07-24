package interview.textinterview.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;

@Builder
@Schema(description = "面试问答历史项")
public record InterviewHistoryItemVO(
        @Schema(description = "作答记录ID")
        Long questionId,

        @Schema(description = "题目索引号")
        Integer questionIndex,

        @Schema(description = "AI 提出的问题原文")
        String questionText,

        @Schema(description = "求职者提交的文本答案")
        String userAnswer,

        @Schema(description = "AI 单题打分 0-100")
        Integer score,

        @Schema(description = "AI 追问或反馈评价")
        String aiFeedback,

        @Schema(description = "提交答案时间")
        OffsetDateTime answeredAt,

        @Schema(description = "追问链路列表")
        List<InterviewHistoryItemVO> followUps
) {
}
