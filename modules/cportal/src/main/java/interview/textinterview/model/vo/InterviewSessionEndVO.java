package interview.textinterview.model.vo;

import interview.common.enums.InterviewReportGenerationStatus;
import interview.common.enums.InterviewSessionStatus;
import interview.common.enums.InterviewType;
import interview.voiceinterview.model.enums.VoiceEvaluationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "结束面试会话响应")
public record InterviewSessionEndVO(
        @Schema(description = "会话ID")
        Long sessionId,

        @Schema(description = "会话类型", example = "TEXT")
        InterviewType sessionType,

        @Schema(description = "会话状态", example = "COMPLETED")
        InterviewSessionStatus status,

        @Schema(description = "实际持续秒数")
        Integer actualDurationSeconds,

        @Schema(description = "报告生成状态", example = "PENDING")
        InterviewReportGenerationStatus reportStatus,

        @Schema(description = "语音评估状态，文本会话为空", nullable = true)
        VoiceEvaluationStatus voiceEvaluationStatus,

        @Schema(description = "结束时间")
        OffsetDateTime endedAt
) {
}
