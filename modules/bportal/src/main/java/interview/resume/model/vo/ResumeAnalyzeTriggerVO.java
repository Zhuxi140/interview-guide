package interview.resume.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * AI 简历解析触发响应（仅返回任务受理信息，不包含解析结果）
 */
@Builder
@Schema(description = "AI 简历解析触发响应")
public record ResumeAnalyzeTriggerVO(
        @Schema(description = "本地消息ID，同时作为本次分析的taskId")
        Long taskId,
        @Schema(description = "简历ID")
        Long resumeId,
        @Schema(description = "当前解析状态")
        interview.resume.model.enums.AnalyzeStatus analyzeStatus
) {
}
