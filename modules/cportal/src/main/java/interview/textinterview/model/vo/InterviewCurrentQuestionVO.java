package interview.textinterview.model.vo;

import interview.common.enums.QuestionKind;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "当前待作答面试题")
public record InterviewCurrentQuestionVO(
        @Schema(description = "题目是否仍在生成中", example = "false")
        Boolean pending,

        @Schema(description = "题目 ID")
        Long questionId,

        @Schema(description = "题目事件序号")
        Long sequence,

        @Schema(description = "题目类型", allowableValues = {"FIRST", "FOLLOW_UP", "INTERVIEWER"})
        QuestionKind questionKind,

        @Schema(description = "题目正文")
        String content,

        @Schema(description = "考察点")
        String assessmentPoint,

        @Schema(description = "难度")
        String difficulty,

        @Schema(description = "追问所基于的答案 ID")
        Long parentAnswerId,

        @Schema(description = "题目签发时间")
        OffsetDateTime issuedAt
) {
}
