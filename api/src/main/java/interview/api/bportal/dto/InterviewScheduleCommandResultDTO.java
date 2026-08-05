package interview.api.bportal.dto;

import interview.common.enums.InterviewScheduleStatus;

import java.time.OffsetDateTime;

/**
 * 面试排期状态变更结果。
 */
public record InterviewScheduleCommandResultDTO(
        Long scheduleId,
        InterviewScheduleStatus status,
        Integer version,
        OffsetDateTime updatedAt
) {
}
