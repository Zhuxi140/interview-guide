package interview.interviewcfg.model.req;

import interview.common.enums.InterviewType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Agent 面试编排草案创建请求")
public record InterviewPlanDraftCreateReq(
        @Positive(message = "模板ID必须大于0")
        @NotNull(message = "模板ID不能为空")
        @Schema(description = "面试模板ID", example = "31001")
        Long templateId,

        @NotNull(message = "面试类型不能为空")
        @Schema(description = "面试类型；第三阶段仅支持TEXT", example = "TEXT")
        InterviewType interviewType,

        @NotNull(message = "面试时长不能为空")
        @Min(value = 15, message = "面试时长不能少于15分钟")
        @Max(value = 480, message = "面试时长不能超过480分钟")
        @Schema(description = "面试总时长（分钟）", example = "60")
        Integer durationMinutes,

        @Min(value = 1, message = "最大轮次数不能小于1")
        @Max(value = 10, message = "最大轮次数不能超过10")
        @Schema(description = "可选的最大轮次数", example = "3")
        Integer maxRounds,

        @Size(max = 20, message = "候选面试官不能超过20人")
        @Schema(description = "可选的候选面试官用户ID")
        List<@Positive(message = "面试官用户ID必须大于0") Long> interviewerUserIds,

        @Size(max = 20, message = "阶段编码不能超过20个")
        @Schema(description = "HR 从模板阶段池选定的有序阶段编码子集；为空时按模板全阶段默认顺序", example = "[\"TECHNICAL\",\"HR\"]")
        List<@NotBlank(message = "阶段编码不能为空") @Pattern(regexp = "^[A-Z][A-Z0-9_]{0,31}$", message = "阶段编码格式无效") String> phaseCodes,

        @Size(max = 1000, message = "编排提示词不能超过1000字")
        @Schema(description = "选填：对 Agent 的补充编排说明，如候选人特点、考察侧重", example = "候选人偏业务，多考察方案设计")
        String prompt
) {
}
