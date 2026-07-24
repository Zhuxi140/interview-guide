package interview.textinterview.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "面试题目")
public record InterviewQuestionVO(
        @Schema(description = "题目ID")
        Long questionId,

        @Schema(description = "题目索引号")
        Integer questionIndex,

        @Schema(description = "AI 提出的问题原文", example = "请介绍一下 Spring Boot 的核心特性")
        String questionText
) {
}
