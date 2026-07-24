package interview.textinterview.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "面试报告查询响应")
public record InterviewReportVO(
        @Schema(description = "报告生成状态 PENDING / PROCESSING / COMPLETED / FAILED")
        String generationStatus,

        @Schema(description = "失败原因（仅 generationStatus=FAILED 时有值）")
        String failureReason,

        @Schema(description = "报告详情（仅 generationStatus=COMPLETED 时有值）")
        InterviewReportDetailVO report
) {
}
