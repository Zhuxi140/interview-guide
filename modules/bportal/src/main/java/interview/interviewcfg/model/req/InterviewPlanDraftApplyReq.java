package interview.interviewcfg.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "应用 Agent 面试编排草案请求")
public record InterviewPlanDraftApplyReq(
        @NotNull(message = "乐观锁版本号不能为空")
        @Min(value = 0, message = "乐观锁版本号不能小于0")
        @Schema(description = "客户端读取到的草案版本", example = "3")
        Integer expectedVersion,

        @Valid
        @NotNull(message = "修订后的面试计划不能为空")
        @Schema(description = "HR 修订后的完整面试计划；阶段结构与顺序必须与草案快照一致")
        Plan plan
) {

    @Schema(description = "HR 修订后的面试计划")
    public record Plan(
            @Valid
            @NotNull(message = "阶段计划不能为空")
            @Size(min = 1, max = 10, message = "阶段数量必须为1到10个")
            @Schema(description = "阶段计划；与草案快照同序")
            List<Stage> stages,

            @Valid
            @Size(max = 20, message = "排期建议不能超过20条")
            @Schema(description = "选中并修订后的排期建议；可为空表示本轮不建排期")
            List<Suggestion> scheduleSuggestions
    ) {
    }

    @Schema(description = "单阶段计划")
    public record Stage(
            @NotBlank(message = "阶段编码不能为空")
            @Pattern(regexp = "^[A-Z][A-Z0-9_]{0,31}$", message = "阶段编码格式无效")
            @Schema(description = "阶段编码；必须属于草案快照且顺序一致", example = "TECHNICAL")
            String phaseCode,

            @NotBlank(message = "考察目标不能为空")
            @Size(max = 2000, message = "考察目标不能超过2000字")
            @Schema(description = "考察目标", example = "考察业务方案设计与沟通")
            String objectives,

            @NotBlank(message = "题纲不能为空")
            @Size(max = 4000, message = "题纲不能超过4000字")
            @Schema(description = "题纲")
            String questionOutline,

            @NotNull(message = "建议时长不能为空")
            @Min(value = 15, message = "建议时长不能少于15分钟")
            @Max(value = 480, message = "建议时长不能超过480分钟")
            @Schema(description = "建议时长（分钟）", example = "60")
            Integer durationMinutes
    ) {
    }

    @Schema(description = "单条排期建议")
    public record Suggestion(
            @NotNull(message = "建议ID不能为空")
            @Positive(message = "建议ID必须大于0")
            @Schema(description = "建议ID；来自草案或HR新增", example = "1")
            Long suggestionId,

            @NotNull(message = "面试官用户ID不能为空")
            @Positive(message = "面试官用户ID必须大于0")
            @Schema(description = "面试官用户ID", example = "5001")
            Long interviewerUserId,

            @NotNull(message = "面试时间不能为空")
            @Schema(description = "面试时间")
            OffsetDateTime interviewTime,

            @Size(max = 256, message = "理由不能超过256字")
            @Schema(description = "理由")
            String reason
    ) {
    }
}
