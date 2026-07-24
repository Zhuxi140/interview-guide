package interview.interviewcfg.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "创建或替换阶段组卷策略请求")
public record PhaseConfigUpsertReq(
        @NotNull(message = "题目数量不能为空")
        @Min(value = 1, message = "最少1题")
        @Max(value = 20, message = "最多20题")
        @Schema(description = "该阶段计划生成的深度题目总数", example = "5")
        Integer questionCount,

        @NotNull(message = "难度权重不能为空")
        @Min(value = 0, message = "权重不能小于0")
        @Max(value = 1, message = "权重不能大于1")
        @Schema(description = "难度权重系数 0.0~1.0", example = "0.5")
        Double difficultyWeight,

        @Schema(description = "大模型 Prompt 覆写策略")
        String promptOverride
) {
}
