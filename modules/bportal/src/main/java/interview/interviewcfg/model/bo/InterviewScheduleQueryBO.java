package interview.interviewcfg.model.bo;

import interview.common.enums.InterviewScheduleStatus;
import interview.common.enums.InterviewType;

import java.time.OffsetDateTime;

/**
 * 面试排期关联查询结果（两表互联：排期 + 投递）。
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
