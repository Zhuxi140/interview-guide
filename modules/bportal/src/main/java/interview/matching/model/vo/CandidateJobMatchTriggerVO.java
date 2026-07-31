package interview.matching.model.vo;

import interview.common.enums.AiTaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 候选人岗位适配预测任务受理响应。
 */
@Schema(description = "候选人岗位适配预测任务受理响应")
public record CandidateJobMatchTriggerVO(
        @Schema(description = "任务 ID")
        Long taskId,
        @Schema(description = "投递 ID")
        Long applicationId,
        @Schema(description = "任务状态")
        AiTaskStatus status
) {
}
