package interview.interviewcfg.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "阶段节点项")
public record StageVO(
        @Schema(description = "阶段编码 INTRO / TECH / PROJECT / HR")
        String phaseCode,

        @Schema(description = "阶段名称", example = "技术面")
        String phaseName,

        @Schema(description = "排序序号")
        Integer sortOrder
) {
}
