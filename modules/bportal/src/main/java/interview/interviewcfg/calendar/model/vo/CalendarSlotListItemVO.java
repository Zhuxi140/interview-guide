package interview.interviewcfg.calendar.model.vo;

import interview.interviewcfg.calendar.model.enums.CalendarSlotStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * 面试官日历空闲时段列表项。
 */
@Schema(description = "面试官日历空闲时段列表项")
public record CalendarSlotListItemVO(
        @Schema(description = "时段 ID")
        Long id,
        @Schema(description = "面试官系统用户 ID")
        Long interviewerUserId,
        @Schema(description = "空闲时段开始时间（ISO-8601 带时区）")
        OffsetDateTime slotStart,
        @Schema(description = "空闲时段结束时间（ISO-8601 带时区）")
        OffsetDateTime slotEnd,
        @Schema(description = "时段状态", example = "AVAILABLE")
        CalendarSlotStatus status,
        @Schema(description = "乐观锁版本号", example = "0")
        Integer version,
        @Schema(description = "创建时间")
        OffsetDateTime createdAt
) {
}
