package interview.interviewcfg.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;

@Builder
@Schema(description = "面试模板详情（含阶段序列与组卷策略）")
public record InterviewTemplateDetailVO(
        @Schema(description = "模板ID")
        Long id,

        @Schema(description = "流程模板名称")
        String templateName,

        @Schema(description = "阶段节点列表")
        List<StageVO> stages,

        @Schema(description = "组卷策略列表")
        List<PhaseConfigVO> phaseConfigs,

        @Schema(description = "版本号")
        Integer version,

        @Schema(description = "创建时间")
        OffsetDateTime createdAt,

        @Schema(description = "更新时间")
        OffsetDateTime updatedAt
) {
}
