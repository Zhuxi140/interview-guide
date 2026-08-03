package interview.interviewcfg.model.bo;

import interview.common.enums.InterviewScheduleStatus;

import java.time.OffsetDateTime;

/**
 * 面试排期关联查询结果。
 */
public record InterviewScheduleQueryBO(
        Long id,
        Long enterpriseId,
        Long applicationId,
        Long candidateUserId,
        Long jobId,
        Long templateId,
        Short roundNo,
        String phaseCode,
        String phaseName,
        Long interviewerUserId,
        String jobTitle,
        OffsetDateTime interviewTime,
        Integer durationMinutes,
        String interviewType,
        InterviewScheduleStatus status,
        String statusReason,
        Integer version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
