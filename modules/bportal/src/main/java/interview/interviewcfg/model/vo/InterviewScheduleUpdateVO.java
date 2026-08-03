package interview.interviewcfg.model.vo;

import interview.common.enums.InterviewScheduleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "面试排期更新/取消/流转通用响应")
public record InterviewScheduleUpdateVO(
        @Schema(description = "排期ID")
        Long id,

        @Schema(description = "排期状态")
        InterviewScheduleStatus status,

        @Schema(description = "面试时间")
        OffsetDateTime interviewTime,

        @Schema(description = "预计面试时长（分钟）", example = "60")
        Integer durationMinutes,

        @Schema(description = "乐观锁版本号")
        Integer version,

        @Schema(description = "更新时间")
        OffsetDateTime updatedAt
) {
}
