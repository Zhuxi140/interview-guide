package interview.interviewcfg.model.req;

import interview.common.enums.InterviewType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

@Schema(description = "HR 创建面试排期请求")
public record InterviewScheduleCreateReq(
        @NotNull(message = "模板ID不能为空")
        @Schema(description = "面试阶段模板ID", example = "2001")
        Long templateId,

        @NotNull(message = "面试轮次不能为空")
        @Min(value = 1, message = "面试轮次必须大于0")
        @Schema(description = "当前投递下的面试轮次", example = "1")
        Short roundNo,

        @NotNull(message = "面试官用户ID不能为空")
        @Schema(description = "面试官用户ID（为空则系统自动分配）", example = "3001")
        Long interviewerUserId,

        @NotNull(message = "面试时间不能为空")
        @Schema(description = "排期面试时间（ISO-8601）", example = "2026-08-01T14:00:00+08:00")
        OffsetDateTime interviewTime,

        @Min(value = 15, message = "预计时长不能少于15分钟")
        @Max(value = 480, message = "预计时长不能超过480分钟")
        @Schema(description = "预计面试时长（分钟），不传时默认60", example = "60")
        Integer durationMinutes,

        @NotNull(message = "面试类型不能为空")
        @Schema(description = "面试类型 TEXT / VOICE", example = "TEXT")
        InterviewType interviewType
) {
}
