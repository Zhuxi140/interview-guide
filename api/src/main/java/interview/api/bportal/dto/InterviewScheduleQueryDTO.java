package interview.api.bportal.dto;

import interview.common.enums.InterviewScheduleStatus;

import java.time.OffsetDateTime;

/**
 * 面试排期只读 DTO。
 *
 * @param id 排期 ID
 * @param enterpriseId 企业 ID
 * @param applicationId 投递 ID
 * @param candidateUserId 候选人用户 ID
 * @param jobId 岗位 ID
 * @param templateId 模板 ID
 * @param roundNo 当前投递下的面试轮次
 * @param phaseCode 模板阶段编码
 * @param phaseName 模板阶段名称
 * @param interviewerUserId 面试官用户 ID
 * @param candidateName 候选人名称
 * @param enterpriseName 企业名称
 * @param jobTitle 岗位名称
 * @param interviewTime 面试时间
 * @param durationMinutes 预计面试时长（分钟）
 * @param interviewType 面试类型
 * @param status 排期状态
 * @param statusReason 状态原因
 * @param version 乐观锁版本号
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
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
        String interviewType,
        InterviewScheduleStatus status,
        String statusReason,
        Integer version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
