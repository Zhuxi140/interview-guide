package interview.interviewcfg.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

@Schema(description = "HR 创建面试排期请求")
public record InterviewScheduleCreateReq(
        @NotNull(message = "投递ID不能为空")
        @Schema(description = "投递记录ID", example = "1001")
        Long applicationId,

        @NotNull(message = "模板ID不能为空")
        @Schema(description = "面试阶段模板ID", example = "2001")
        Long templateId,

        @Schema(description = "面试官用户ID（为空则系统自动分配）", example = "3001")
        Long interviewerUserId,

        @NotNull(message = "面试时间不能为空")
        @Schema(description = "排期面试时间（ISO-8601）", example = "2026-08-01T14:00:00+08:00")
        OffsetDateTime interviewTime,

        @NotBlank(message = "面试类型不能为空")
        @Schema(description = "面试类型 TEXT / VOICE / CODE", example = "TEXT")
        String interviewType
) {
}
