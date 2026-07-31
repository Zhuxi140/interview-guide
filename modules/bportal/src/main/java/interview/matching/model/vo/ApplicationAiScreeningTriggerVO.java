package interview.matching.model.vo;

import interview.common.enums.AiTaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * HR AI 初筛任务受理响应。
 */
@Schema(description = "HR AI 初筛任务受理响应")
public record ApplicationAiScreeningTriggerVO(
        @Schema(description = "任务 ID")
        Long taskId,
        @Schema(description = "投递 ID")
        Long applicationId,
        @Schema(description = "任务状态")
        AiTaskStatus status
) {
}
