package interview.interviewcfg.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "阶段组卷策略")
public record PhaseConfigVO(
        @Schema(description = "配置主键")
        Long id,

        @Schema(description = "阶段编码 INTRO / TECH / PROJECT / HR")
        String phaseCode,

        @Schema(description = "该阶段计划生成的深度题目总数")
        Integer questionCount,

        @Schema(description = "难度权重系数 0.0~1.0")
        Double difficultyWeight,

        @Schema(description = "大模型 Prompt 覆写策略")
        String promptOverride,

        @Schema(description = "更新时间")
        OffsetDateTime updatedAt
) {
}
