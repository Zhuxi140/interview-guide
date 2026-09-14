package interview.textinterview.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 作答提交结果（U3-08）。
 *
 * @param answerId         作答记录 ID
 * @param score            评分（0-100）；采用异步评分策略时为 null
 * @param aiFeedback       AI 反馈；同 score，异步策略下为 null
 * @param followUpGenerated 是否已触发追问题生成
 * @param sessionEnded     本次作答后是否会话结束
 */
@Schema(description = "作答提交结果")
public record InterviewAnswerSubmitVO(
        @Schema(description = "作答记录 ID")
        Long answerId,

        @Schema(description = "评分（0-100），异步评分时为 null")
        Integer score,

        @Schema(description = "AI 反馈，异步评分时为 null")
        String aiFeedback,

        @Schema(description = "是否已触发追问生成")
        Boolean followUpGenerated,

        @Schema(description = "会话是否已结束")
        Boolean sessionEnded
) {
}
