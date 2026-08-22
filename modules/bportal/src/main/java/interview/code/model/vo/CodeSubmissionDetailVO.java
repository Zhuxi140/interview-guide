package interview.code.model.vo;

import interview.code.model.enums.CodeExecutionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

/**
 * 候选人查询指定提交的执行与 AI 审查结果响应。
 */
@Builder
@Schema(description = "候选人提交详情响应")
public record CodeSubmissionDetailVO(
        @Schema(description = "提交记录 ID")
        Long submissionId,
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
        @Schema(description = "AI 审查状态（PENDING/COMPLETED/FAILED）；审查链路未接入时为 null")
        String aiReviewStatus,
        @Schema(description = "AI 审查结果；未完成时为 null")
        AiReviewVO aiReview
) {
}
