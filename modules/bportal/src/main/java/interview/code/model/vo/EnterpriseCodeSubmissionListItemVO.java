package interview.code.model.vo;

import interview.code.model.enums.CodeExecutionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * 企业侧候选人提交记录列表项响应。
 */
@Builder
@Schema(description = "企业侧候选人提交记录列表项响应")
public record EnterpriseCodeSubmissionListItemVO(
        @Schema(description = "提交记录 ID")
        Long submissionId,
        @Schema(description = "面试会话 ID")
        Long sessionId,
        @Schema(description = "候选人用户 ID")
        Long candidateId,
        @Schema(description = "题目 ID")
        Long questionId,
        @Schema(description = "题目名称")
        String questionTitle,
        @Schema(description = "编程语言", example = "JAVA")
        String language,
        @Schema(description = "执行状态")
        CodeExecutionStatus executionStatus,
        @Schema(description = "通过用例数", example = "8")
        Long passedCount,
        @Schema(description = "总用例数", example = "10")
        Long totalCount,
        @Schema(description = "该题目在当前会话内的第几次提交（从 1 开始）", example = "1")
        Long attemptNumber,
        @Schema(description = "提交时间")
        OffsetDateTime submittedAt
) {
}
