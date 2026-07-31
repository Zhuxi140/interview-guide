package interview.matching.service;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.aicore.dto.AiDimensionMatch;
import interview.api.aicore.dto.AiJobRequirementInput;
import interview.api.infra.LocalMessageApi;
import interview.api.infra.dto.MessageDTO;
import interview.api.system.EnterpriseValidationApi;
import interview.common.enums.AiTaskStatus;
import interview.common.enums.ErrorCode;
import interview.common.enums.MsgPriority;
import interview.common.enums.MsgStatus;
import interview.common.enums.MsgTopic;
import interview.common.exception.BusinessException;
import interview.common.util.TraceUtil;
import interview.framework.context.AuthContext;
import interview.job.model.entity.Job;
import interview.job.service.JobService;
import interview.matching.event.ApplicationScreeningCreatedEvent;
import interview.matching.mapper.ApplicationAiScreeningMapper;
import interview.matching.mapper.JobApplicationsMapper;
import interview.matching.model.bo.ScreeningThresholdSnapshot;
import interview.matching.model.entity.ApplicationAiScreening;
import interview.matching.model.entity.JobApplications;
import interview.matching.model.entity.JobScreeningConfig;
import interview.matching.model.enums.JobApplicationStatus;
import interview.matching.model.message.ApplicationScreeningCommand;
import interview.matching.model.req.ApplicationAiReviewReq;
import interview.matching.model.vo.ApplicationAiScreeningReviewVO;
import interview.matching.model.vo.ApplicationAiScreeningTriggerVO;
import interview.matching.model.vo.ApplicationAiScreeningVO;
import interview.resume.mapper.CandidateProfileMapper;
import interview.resume.mapper.CandidateSkillScoresMapper;
import interview.resume.model.entity.CandidateProfile;
import interview.resume.model.entity.CandidateSkillScores;
import interview.resume.model.vo.CandidateProfileVO;
import interview.resume.model.vo.CandidateSkillScoreVO;
import interview.resume.service.CandidateProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 受理 HR 初筛、查询结果并原子提交人工审核。
 */
@Service
@RequiredArgsConstructor
public class ApplicationAiScreeningServiceImpl
        extends ServiceImpl<ApplicationAiScreeningMapper, ApplicationAiScreening>
        implements ApplicationAiScreeningService {

    private static final long MESSAGE_FALLBACK_SECONDS = 300;
    private static final String MESSAGE_BIZ_PREFIX = "HR_APPLICATION_SCREENING:";

    private final JobApplicationsMapper jobApplicationsMapper;
    private final JobScreeningConfigService jobScreeningConfigService;
    private final JobService jobService;
    private final CandidateProfileService candidateProfileService;
    private final CandidateProfileMapper candidateProfileMapper;
    private final CandidateSkillScoresMapper candidateSkillScoresMapper;
    private final MatchingAiInputService matchingAiInputService;
    private final EnterpriseValidationApi enterpriseValidationApi;
    private final LocalMessageApi localMessageApi;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ApplicationAiScreeningTriggerVO accept(
            Long enterpriseId, Long applicationId, String idempotencyKey) {
        Long userId = AuthContext.getRequiredUserId();
        enterpriseValidationApi.validateEnterpriseBelong(enterpriseId, userId);
        JobApplications application = findApplicationForUpdate(
                enterpriseId, applicationId);
        return acceptInternal(application, idempotencyKey, userId, false);
    }

    @Override
    @Transactional
    public void acceptAutomatic(JobApplications application) {
        JobScreeningConfig config = jobScreeningConfigService.lambdaQuery()
                .eq(JobScreeningConfig::getJobId, application.getJobId())
                .eq(JobScreeningConfig::getEnterpriseId, application.getEnterpriseId())
                .eq(JobScreeningConfig::getEnabled, true)
                .one();
        if (config != null) {
            acceptInternal(application,
                    "AUTO:" + application.getId() + ":" + config.getVersion(),
                    application.getCandidateId(), true);
            application.setStatus(JobApplicationStatus.REVIEWING);
        }
    }

    @Override
    public ApplicationAiScreeningVO getLatest(
            Long enterpriseId, Long applicationId) {
        validateEnterpriseApplication(enterpriseId, applicationId);
        ApplicationAiScreening screening = lambdaQuery()
                .eq(ApplicationAiScreening::getApplicationId, applicationId)
                .orderByDesc(ApplicationAiScreening::getCreatedAt)
                .last("LIMIT 1")
                .one();
        if (screening == null) {
            throw new BusinessException(ErrorCode.APPLICATION_AI_SCREENING_NOT_FOUND);
        }
        return toVO(screening);
    }

    @Override
    public CandidateProfileVO getCandidateProfile(
            Long enterpriseId, Long applicationId) {
        validateEnterpriseApplication(enterpriseId, applicationId);
        ApplicationAiScreening screening = lambdaQuery()
                .select(ApplicationAiScreening::getCandidateProfileId)
                .eq(ApplicationAiScreening::getApplicationId, applicationId)
                .isNotNull(ApplicationAiScreening::getCandidateProfileId)
                .orderByDesc(ApplicationAiScreening::getCreatedAt)
                .last("LIMIT 1")
                .one();
        if (screening == null) {
            throw new BusinessException(ErrorCode.APPLICATION_AI_SCREENING_NOT_FOUND);
        }
        CandidateProfile profile = candidateProfileMapper.selectById(
                screening.getCandidateProfileId());
        if (profile == null) {
            throw new BusinessException(ErrorCode.CANDIDATE_PROFILE_STATUS_INVALID);
        }
        List<CandidateSkillScoreVO> dimensions = profile.getStatus()
                == AiTaskStatus.COMPLETED
                ? candidateSkillScoresMapper.selectList(
                        Wrappers.<CandidateSkillScores>lambdaQuery()
                                .eq(CandidateSkillScores::getCandidateProfileId,
                                        profile.getId())
                                .orderByAsc(CandidateSkillScores::getDimensionCode))
                        .stream().map(this::toDimensionVO).toList()
                : List.of();
        return new CandidateProfileVO(
                profile.getId(), profile.getResumeId(), profile.getStatus(),
                profile.getProfileSchemaVersion(), readSummary(profile.getSummaryJson()),
                dimensions, profile.getAnalyzedAt());
    }

    @Override
    @Transactional
    public ApplicationAiScreeningReviewVO review(
            Long enterpriseId, Long applicationId,
            Long screeningId, ApplicationAiReviewReq req) {
        Long userId = AuthContext.getRequiredUserId();
        enterpriseValidationApi.validateEnterpriseBelong(enterpriseId, userId);
        findApplicationForUpdate(enterpriseId, applicationId);
        if (req.getExpectedApplicationStatus() != JobApplicationStatus.REVIEWING
                || (req.getDecision() != JobApplicationStatus.PASSED
                && req.getDecision() != JobApplicationStatus.REJECTED)) {
            throw new BusinessException(ErrorCode.JOB_APPLICATION_STATUS_INVALID);
        }
        ApplicationAiScreening screening = lambdaQuery()
                .eq(ApplicationAiScreening::getId, screeningId)
                .eq(ApplicationAiScreening::getApplicationId, applicationId)
                .one();
        if (screening == null) {
            throw new BusinessException(ErrorCode.APPLICATION_AI_SCREENING_NOT_FOUND);
        }
        if (screening.getStatus() != AiTaskStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.APPLICATION_AI_SCREENING_STATUS_INVALID);
        }
        if (screening.getReviewDecision() != null) {
            throw new BusinessException(ErrorCode.APPLICATION_AI_SCREENING_ALREADY_REVIEWED);
        }

        OffsetDateTime now = OffsetDateTime.now();
        int applicationUpdated = jobApplicationsMapper.update(null,
                Wrappers.<JobApplications>lambdaUpdate()
                        .eq(JobApplications::getId, applicationId)
                        .eq(JobApplications::getEnterpriseId, enterpriseId)
                        .eq(JobApplications::getStatus,
                                req.getExpectedApplicationStatus())
                        .set(JobApplications::getStatus, req.getDecision())
                        .set(JobApplications::getUpdatedBy, userId)
                        .set(JobApplications::getTraceId, TraceUtil.getTraceId())
                        .set(JobApplications::getUpdatedAt, now));
        if (applicationUpdated != 1) {
            throw new BusinessException(ErrorCode.JOB_APPLICATION_STATUS_INVALID);
        }

        ApplicationAiScreening reviewPatch = ApplicationAiScreening.builder()
                .reviewDecision(req.getDecision())
                .reviewedBy(userId)
                .reviewedAt(now)
                .traceId(TraceUtil.getTraceId())
                .updatedAt(now)
                .build();
        int screeningUpdated = baseMapper.update(reviewPatch,
                Wrappers.<ApplicationAiScreening>lambdaUpdate()
                        .eq(ApplicationAiScreening::getId, screeningId)
                        .eq(ApplicationAiScreening::getApplicationId, applicationId)
                        .eq(ApplicationAiScreening::getStatus, AiTaskStatus.COMPLETED)
                        .isNull(ApplicationAiScreening::getReviewDecision));
        if (screeningUpdated != 1) {
            throw new BusinessException(ErrorCode.APPLICATION_AI_SCREENING_ALREADY_REVIEWED);
        }
        return new ApplicationAiScreeningReviewVO(
                screeningId, applicationId, req.getDecision(), now);
    }

    private ApplicationAiScreeningTriggerVO acceptInternal(
            JobApplications application, String idempotencyKey,
            Long actorId, boolean automatic) {
        if (application.getStatus() != JobApplicationStatus.APPLIED
                && application.getStatus() != JobApplicationStatus.REVIEWING) {
            throw new BusinessException(ErrorCode.JOB_APPLICATION_STATUS_INVALID);
        }
        JobScreeningConfig config = jobScreeningConfigService.lambdaQuery()
                .eq(JobScreeningConfig::getJobId, application.getJobId())
                .eq(JobScreeningConfig::getEnterpriseId, application.getEnterpriseId())
                .one();
        if (config == null || (automatic && !Boolean.TRUE.equals(config.getEnabled()))) {
            throw new BusinessException(ErrorCode.JOB_SCREENING_CONFIG_NOT_FOUND);
        }

        String keyHash = DigestUtil.sha256Hex(application.getEnterpriseId()
                + ":" + application.getId() + ":" + idempotencyKey);
        ApplicationAiScreening repeated = lambdaQuery()
                .eq(ApplicationAiScreening::getApplicationId, application.getId())
                .eq(ApplicationAiScreening::getIdempotencyKeyHash, keyHash)
                .one();
        if (repeated != null) {
            return new ApplicationAiScreeningTriggerVO(
                    repeated.getId(), application.getId(), repeated.getStatus());
        }
        boolean active = lambdaQuery()
                .eq(ApplicationAiScreening::getApplicationId, application.getId())
                .in(ApplicationAiScreening::getStatus,
                        AiTaskStatus.WAITING_PROFILE,
                        AiTaskStatus.PENDING,
                        AiTaskStatus.PROCESSING)
                .exists();
        if (active) {
            throw new BusinessException(ErrorCode.APPLICATION_AI_SCREENING_STATUS_INVALID);
        }

        CandidateProfile profile = candidateProfileService.ensureProfile(
                application.getResumeId(), application.getCandidateId(),
                application.getId(), application.getEnterpriseId(), actorId);
        Job job = jobService.lambdaQuery()
                .eq(Job::getId, application.getJobId())
                .eq(Job::getEnterpriseId, application.getEnterpriseId())
                .one();
        if (job == null) {
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND);
        }
        AiJobRequirementInput jobSnapshot = matchingAiInputService.toJobSnapshot(job);
        ScreeningThresholdSnapshot thresholdSnapshot =
                new ScreeningThresholdSnapshot(config.getOverallThreshold(),
                        readThresholds(config.getDimensionThresholds()));
        ApplicationScreeningCommand command =
                new ApplicationScreeningCommand(application.getId());
        OffsetDateTime now = OffsetDateTime.now();
        String payload = writeJson(command);
        MessageDTO message = MessageDTO.builder()
                .bizKey(MESSAGE_BIZ_PREFIX + application.getId() + ":" + keyHash)
                .topic(MsgTopic.HR_APPLICATION_SCREENING)
                .schemaVersion(1)
                .payload(payload)
                .status(MsgStatus.PENDING)
                .priority(MsgPriority.HIGH)
                .maxRetries(5)
                .nextRetryAt(now.plusSeconds(MESSAGE_FALLBACK_SECONDS))
                .build();
        Long messageId = localMessageApi.saveInCurrentTransaction(message);
        AiTaskStatus status = profile.getStatus() == AiTaskStatus.COMPLETED
                ? AiTaskStatus.PENDING : AiTaskStatus.WAITING_PROFILE;
        ApplicationAiScreening screening = ApplicationAiScreening.builder()
                .id(messageId)
                .applicationId(application.getId())
                .candidateProfileId(profile.getId())
                .status(status)
                .thresholdSnapshot(writeJson(thresholdSnapshot))
                .jobSnapshot(writeJson(jobSnapshot))
                .attemptCount(0)
                .idempotencyKeyHash(keyHash)
                .traceId(TraceUtil.getTraceId())
                .createdAt(now)
                .updatedAt(now)
                .build();
        if (baseMapper.insert(screening) != 1) {
            throw new BusinessException(
                    ErrorCode.APPLICATION_AI_SCREENING_STATUS_INVALID);
        }

        if (application.getStatus() == JobApplicationStatus.APPLIED) {
            int updated = jobApplicationsMapper.update(null,
                    Wrappers.<JobApplications>lambdaUpdate()
                            .eq(JobApplications::getId, application.getId())
                            .eq(JobApplications::getEnterpriseId,
                                    application.getEnterpriseId())
                            .eq(JobApplications::getStatus, JobApplicationStatus.APPLIED)
                            .set(JobApplications::getStatus,
                                    JobApplicationStatus.REVIEWING)
                            .set(JobApplications::getUpdatedBy, actorId)
                            .set(JobApplications::getTraceId, TraceUtil.getTraceId())
                            .set(JobApplications::getUpdatedAt, now));
            if (updated != 1) {
                throw new BusinessException(ErrorCode.JOB_APPLICATION_STATUS_INVALID);
            }
        }
        eventPublisher.publishEvent(
                new ApplicationScreeningCreatedEvent(messageId, application.getId()));
        return new ApplicationAiScreeningTriggerVO(
                messageId, application.getId(), status);
    }

    private JobApplications findApplication(Long enterpriseId, Long applicationId) {
        JobApplications application = jobApplicationsMapper.selectOne(
                Wrappers.<JobApplications>lambdaQuery()
                        .select(JobApplications::getId,
                                JobApplications::getEnterpriseId,
                                JobApplications::getJobId,
                                JobApplications::getCandidateId,
                                JobApplications::getResumeId,
                                JobApplications::getStatus)
                        .eq(JobApplications::getId, applicationId)
                        .eq(JobApplications::getEnterpriseId, enterpriseId));
        if (application == null) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_FOUND);
        }
        return application;
    }

    private JobApplications findApplicationForUpdate(
            Long enterpriseId, Long applicationId) {
        JobApplications application =
                jobApplicationsMapper.selectByIdForUpdate(applicationId);
        if (application == null
                || !enterpriseId.equals(application.getEnterpriseId())) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_FOUND);
        }
        return application;
    }

    private void validateEnterpriseApplication(
            Long enterpriseId, Long applicationId) {
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());
        findApplication(enterpriseId, applicationId);
    }

    private ApplicationAiScreeningVO toVO(ApplicationAiScreening screening) {
        return new ApplicationAiScreeningVO(
                screening.getId(), screening.getApplicationId(),
                screening.getCandidateProfileId(), screening.getStatus(),
                screening.getOverallMatchScore(),
                readDimensionMatches(screening.getDimensionMatchesJson()),
                screening.getRecommendation(), screening.getReviewDecision(),
                screening.getReviewedBy(), screening.getReviewedAt(),
                screening.getFailureReason(), screening.getCreatedAt());
    }

    private CandidateSkillScoreVO toDimensionVO(CandidateSkillScores score) {
        return new CandidateSkillScoreVO(
                score.getDimensionCode(), score.getScore(),
                score.getAiJustification(), readStringList(score.getEvidenceJson()));
    }

    private String readSummary(String json) {
        if (json == null) {
            return null;
        }
        try {
            Map<?, ?> value = objectMapper.readValue(json, Map.class);
            return value.get("summary") == null
                    ? null : value.get("summary").toString();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }
    }

    private List<String> readStringList(String json) {
        if (json == null) {
            return List.of();
        }
        try {
            return Arrays.asList(objectMapper.readValue(json, String[].class));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }
    }

    private List<AiDimensionMatch> readDimensionMatches(String json) {
        if (json == null) {
            return List.of();
        }
        try {
            return Arrays.asList(
                    objectMapper.readValue(json, AiDimensionMatch[].class));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<interview.common.enums.CandidateDimensionCode, Integer>
    readThresholds(String json) {
        try {
            Map<String, Object> raw = objectMapper.readValue(json, Map.class);
            Map<interview.common.enums.CandidateDimensionCode, Integer> result =
                    new java.util.EnumMap<>(
                            interview.common.enums.CandidateDimensionCode.class);
            raw.forEach((key, value) -> result.put(
                    interview.common.enums.CandidateDimensionCode.valueOf(key),
                    ((Number) value).intValue()));
            return result;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR);
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
