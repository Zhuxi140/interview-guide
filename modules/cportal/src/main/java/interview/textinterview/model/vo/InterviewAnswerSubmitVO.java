package interview.textinterview.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "提交答案响应")
public record InterviewAnswerSubmitVO(
        @Schema(description = "作答记录ID")
        Long answerId,

        @Schema(description = "当前题目索引")
        Integer questionIndex,

        @Schema(description = "AI 单题打分 0-100")
        Integer score,

        @Schema(description = "AI 追问或反馈评价")
        String aiFeedback,

        @Schema(description = "下一道题目（null 表示面试结束或无更多题目）")
        InterviewQuestionVO nextQuestion,

        @Schema(description = "是否为最后一道题")
        Boolean isLastQuestion,

        @Schema(description = "会话版本号")
        Integer sessionVersion
) {
}
