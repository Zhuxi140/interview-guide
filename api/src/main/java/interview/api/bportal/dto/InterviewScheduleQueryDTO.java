package interview.api.bportal.dto;

import interview.common.enums.InterviewScheduleStatus;
import interview.common.enums.InterviewType;

import java.time.OffsetDateTime;

/**
 * 面试排期只读 DTO。
 */
public record InterviewScheduleQueryDTO(
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
        String candidateName,
        String enterpriseName,
        String jobTitle,
        OffsetDateTime interviewTime,
        Integer durationMinutes,
        InterviewType interviewType,
        InterviewScheduleStatus status,
        String statusReason,
        Integer version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
