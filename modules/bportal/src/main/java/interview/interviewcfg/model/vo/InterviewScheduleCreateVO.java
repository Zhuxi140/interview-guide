package interview.interviewcfg.model.vo;

import interview.common.enums.InterviewScheduleStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "创建面试排期响应")
public record InterviewScheduleCreateVO(
        @Schema(description = "排期ID")
        Long id,

        @Schema(description = "投递记录ID")
        Long applicationId,

        @Schema(description = "当前投递下的面试轮次", example = "1")
        Short roundNo,

        @Schema(description = "由模板轮次派生的阶段编码", example = "TECHNICAL")
        String phaseCode,

        @Schema(description = "阶段名称", example = "技术面")
        String phaseName,

        @Schema(description = "排期状态", example = "PENDING_CONFIRMATION")
        InterviewScheduleStatus status,

        @Schema(description = "乐观锁版本号")
        Integer version,

        @Schema(description = "面试时间")
        OffsetDateTime interviewTime,

        @Schema(description = "预计面试时长（分钟）", example = "60")
        Integer durationMinutes,

        @Schema(description = "创建时间")
        OffsetDateTime createdAt
) {
}
