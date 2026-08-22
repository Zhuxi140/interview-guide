package interview.textinterview.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 面试作答记录列表项响应（REST 兜底：断线后恢复现场）
 */
@Schema(description = "面试作答记录列表项响应")
public record InterviewAnswerListItemVO(

        @Schema(description = "作答ID")
        Long answerId,

        @Schema(description = "题目序号（从 1 递增）")
        Integer questionIndex,

        @Schema(description = "题干")
        String questionText,

        @Schema(description = "追问链父消息ID；首题为空")
        Long parentMessageId,

        @Schema(description = "追问深度；首题为 0")
        Integer followUpDepth,

        @Schema(description = "候选人作答内容")
        String content,

        @Schema(description = "AI 评分；未评分为空")
        Integer score,

        @Schema(description = "AI 反馈；未反馈为空")
        String aiFeedback,

        @Schema(description = "作答时间")
        OffsetDateTime submittedAt
) {
}
