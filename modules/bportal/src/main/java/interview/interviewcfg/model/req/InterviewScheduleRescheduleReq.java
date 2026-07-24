package interview.interviewcfg.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

@Schema(description = "HR 重新安排面试请求")
public record InterviewScheduleRescheduleReq(
        @Schema(description = "新的面试官用户ID")
        Long interviewerUserId,

        @NotNull(message = "新的面试时间不能为空")
        @Schema(description = "新的面试时间（ISO-8601）", example = "2026-08-02T10:00:00+08:00")
        OffsetDateTime interviewTime,

        @Schema(description = "重新安排原因", example = "面试官临时有事")
        String transitionReason,

        @NotNull(message = "乐观锁版本号不能为空")
        @Schema(description = "期望版本号", example = "1")
        Integer expectedVersion
) {
}
