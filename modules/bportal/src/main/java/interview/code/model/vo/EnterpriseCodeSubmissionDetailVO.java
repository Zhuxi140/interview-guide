package interview.code.model.vo;

import interview.code.model.enums.CodeExecutionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 企业侧提交详情响应（执行结果 + AI 审查全文 + 提交代码）。
 */
@Builder
@Schema(description = "企业侧提交详情响应")
public record EnterpriseCodeSubmissionDetailVO(
        @Schema(description = "提交记录 ID")
        Long submissionId,
        @Schema(description = "面试会话 ID")
        Long sessionId,
        @Schema(description = "候选人用户 ID")
        Long candidateId,
        @Schema(description = "候选人姓名")
        String candidateName,
        @Schema(description = "题目 ID")
        Long questionId,
        @Schema(description = "题目名称")
        String questionTitle,
        @Schema(description = "编程语言", example = "JAVA")
        String language,
        @Schema(description = "执行状态")
        CodeExecutionStatus executionStatus,
        @Schema(description = "失败原因；仅 TIMEOUT / ERROR 时返回")
        String failureReason,
        @Schema(description = "实际执行耗时（毫秒）", example = "120")
        Integer executionTimeMs,
        @Schema(description = "实际内存占用（MB）", example = "128")
        Integer memoryUsedMb,
        @Schema(description = "通过用例数", example = "8")
        Long passedCount,
        @Schema(description = "总用例数", example = "10")
        Long totalCount,
        @Schema(description = "公开用例逐条执行结果")
        List<CodeCaseResultVO> publicResults,
        @Schema(description = "隐藏用例汇总（不暴露输入与预期输出）")
        HiddenSummaryVO hiddenSummary,
        @Schema(description = "提交的代码原文")
        String submittedCode,
        @Schema(description = "AI 审查状态（PENDING/COMPLETED/FAILED）；审查链路未接入时为 null")
        String aiReviewStatus,
        @Schema(description = "AI 审查结果；未完成时为 null")
        AiReviewVO aiReview,
        @Schema(description = "提交时间")
        OffsetDateTime submittedAt
) {
}
