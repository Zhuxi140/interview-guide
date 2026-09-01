package interview.matching.service;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.aicore.dto.AiJobRequirementInput;
import interview.api.infra.LocalMessageApi;
import interview.api.infra.dto.MessageDTO;
import interview.common.enums.AiTaskStatus;
import interview.common.enums.ErrorCode;
import interview.common.enums.MsgPriority;
import interview.common.enums.MsgStatus;
import interview.common.enums.MsgTopic;
import interview.common.enums.UserType;
import interview.common.exception.BusinessException;
import interview.common.util.TraceUtil;
import interview.framework.context.AuthContext;
import interview.job.model.entity.Job;
import interview.job.service.JobService;
import interview.matching.event.CandidateJobMatchCreatedEvent;
import interview.matching.mapper.CandidateJobMatchAnalysisMapper;
import interview.matching.mapper.JobApplicationsMapper;
import interview.matching.model.entity.CandidateJobMatchAnalysis;
import interview.matching.model.entity.JobApplications;
import interview.matching.model.enums.JobApplicationStatus;
import interview.matching.model.message.CandidateJobMatchCommand;
import interview.matching.model.vo.CandidateJobMatchAnalysisVO;
import interview.matching.model.vo.CandidateJobMatchTriggerVO;
import interview.resume.model.entity.CandidateProfile;
import interview.resume.service.CandidateProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 受理并查询候选人私有岗位适配预测。
 */
@Service
@RequiredArgsConstructor
public class CandidateJobMatchAnalysisServiceImpl
        extends ServiceImpl<CandidateJobMatchAnalysisMapper, CandidateJobMatchAnalysis>
        implements CandidateJobMatchAnalysisService {

    private static final long MESSAGE_FALLBACK_SECONDS = 300;
    private static final String MESSAGE_BIZ_PREFIX = "CANDIDATE_JOB_MATCH:";

    private final JobApplicationsMapper jobApplicationsMapper;
    private final JobService jobService;
    private final CandidateProfileService candidateProfileService;
    private final MatchingAiInputService matchingAiInputService;
    private final LocalMessageApi localMessageApi;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public CandidateJobMatchTriggerVO accept(
            Long applicationId, String idempotencyKey) {
        Long candidateId = requiredCandidateId();
        JobApplications application = findApplicationForUpdate(
                applicationId, candidateId);
        if (application.getStatus() == JobApplicationStatus.WITHDRAWN) {
            throw new BusinessException(ErrorCode.JOB_APPLICATION_STATUS_INVALID);
        }

        String keyHash = DigestUtil.sha256Hex(
                candidateId + ":" + applicationId + ":" + idempotencyKey);
        CandidateJobMatchAnalysis repeated = lambdaQuery()
                .eq(CandidateJobMatchAnalysis::getApplicationId, applicationId)
                .eq(CandidateJobMatchAnalysis::getIdempotencyKeyHash, keyHash)
                .one();
        if (repeated != null) {
            return new CandidateJobMatchTriggerVO(
                    repeated.getId(), applicationId, repeated.getStatus());
        }
        boolean active = lambdaQuery()
                .eq(CandidateJobMatchAnalysis::getApplicationId, applicationId)
                .in(CandidateJobMatchAnalysis::getStatus,
                        AiTaskStatus.WAITING_PROFILE,
                        AiTaskStatus.PENDING,
                        AiTaskStatus.PROCESSING)
                .exists();
        if (active) {
            throw new BusinessException(
                    ErrorCode.CANDIDATE_JOB_MATCH_STATUS_INVALID);
        }

        CandidateProfile profile = candidateProfileService.ensureProfile(
                application.getResumeId(), candidateId,
                applicationId, null, candidateId);
        Job job = jobService.lambdaQuery()
                .eq(Job::getId, application.getJobId())
                .one();
        if (job == null) {
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND);
        }
        AiJobRequirementInput jobSnapshot = matchingAiInputService.toJobSnapshot(job);
        CandidateJobMatchCommand command =
                new CandidateJobMatchCommand(applicationId);
        OffsetDateTime now = OffsetDateTime.now();
        MessageDTO message = MessageDTO.builder()
                .bizKey(MESSAGE_BIZ_PREFIX + applicationId + ":" + keyHash)
                .topic(MsgTopic.CANDIDATE_JOB_MATCHING)
                .schemaVersion(1)
                .payload(writeJson(command))
                .status(MsgStatus.PENDING)
                .priority(MsgPriority.HIGH)
                .maxRetries(5)
                .nextRetryAt(now.plusSeconds(MESSAGE_FALLBACK_SECONDS))
                .build();
        Long messageId = localMessageApi.saveInCurrentTransaction(message);
        AiTaskStatus status = profile.getStatus() == AiTaskStatus.COMPLETED
                ? AiTaskStatus.PENDING : AiTaskStatus.WAITING_PROFILE;
        CandidateJobMatchAnalysis analysis = CandidateJobMatchAnalysis.builder()
                .id(messageId)
                .applicationId(applicationId)
                .candidateProfileId(profile.getId())
                .status(status)
                .jobSnapshot(writeJson(jobSnapshot))
                .attemptCount(0)
                .idempotencyKeyHash(keyHash)
                .traceId(TraceUtil.getTraceId())
                .createdAt(now)
                .updatedAt(now)
                .build();
        if (baseMapper.insert(analysis) != 1) {
            throw new BusinessException(ErrorCode.CANDIDATE_JOB_MATCH_FAILED);
        }
        eventPublisher.publishEvent(new CandidateJobMatchCreatedEvent(
                messageId, applicationId));
        return new CandidateJobMatchTriggerVO(messageId, applicationId, status);
    }

    @Override
    public CandidateJobMatchAnalysisVO getLatest(Long applicationId) {
        Long candidateId = requiredCandidateId();
        findApplication(applicationId, candidateId);
        CandidateJobMatchAnalysis analysis = lambdaQuery()
                .eq(CandidateJobMatchAnalysis::getApplicationId, applicationId)
                .orderByDesc(CandidateJobMatchAnalysis::getCreatedAt)
                .last("LIMIT 1")
                .one();
        if (analysis == null) {
            throw new BusinessException(ErrorCode.CANDIDATE_JOB_MATCH_NOT_FOUND);
        }
        return new CandidateJobMatchAnalysisVO(
                analysis.getId(), analysis.getApplicationId(), analysis.getStatus(),
                analysis.getMatchScore(), analysis.getPassProbability(),
                readList(analysis.getStrengthsJson()), readList(analysis.getGapsJson()),
                analysis.getFailureReason(), analysis.getAnalyzedAt());
    }

    private Long requiredCandidateId() {
        if (AuthContext.getUserType() != UserType.CANDIDATE) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
        return AuthContext.getRequiredUserId();
    }

    private JobApplications findApplication(Long applicationId, Long candidateId) {
        JobApplications application = jobApplicationsMapper.selectOne(
                Wrappers.<JobApplications>lambdaQuery()
                        .select(JobApplications::getId,
                                JobApplications::getEnterpriseId,
                                JobApplications::getJobId,
                                JobApplications::getCandidateId,
                                JobApplications::getResumeId,
                                JobApplications::getStatus)
                        .eq(JobApplications::getId, applicationId)
                        .eq(JobApplications::getCandidateId, candidateId));
        if (application == null) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_FOUND);
        }
        return application;
    }

    private JobApplications findApplicationForUpdate(
            Long applicationId, Long candidateId) {
        JobApplications application =
                jobApplicationsMapper.selectByIdForUpdate(applicationId);
        if (application == null
                || !candidateId.equals(application.getCandidateId())) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_FOUND);
        }
        return application;
    }

    private List<String> readList(String json) {
        if (json == null) {
            return List.of();
        }
        try {
            return Arrays.asList(objectMapper.readValue(json, String[].class));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR);
        }
    }
}
