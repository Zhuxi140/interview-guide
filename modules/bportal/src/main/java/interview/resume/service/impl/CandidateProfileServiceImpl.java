package interview.resume.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.infra.LocalMessageApi;
import interview.api.infra.dto.MessageDTO;
import interview.common.enums.AiTaskStatus;
import interview.common.enums.ErrorCode;
import interview.common.enums.MsgPriority;
import interview.common.enums.MsgStatus;
import interview.common.enums.MsgTopic;
import interview.common.exception.BusinessException;
import interview.common.util.TraceUtil;
import interview.resume.event.CandidateProfileCreatedEvent;
import interview.resume.mapper.CandidateProfileMapper;
import interview.resume.mapper.ResumesMapper;
import interview.resume.model.entity.CandidateProfile;
import interview.resume.model.entity.Resumes;
import interview.resume.model.message.CandidateProfileCommand;
import interview.resume.service.CandidateProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;

/**
 * 查询、复用并可靠创建岗位无关人才画像。
 */
@Service
@RequiredArgsConstructor
public class CandidateProfileServiceImpl
        extends ServiceImpl<CandidateProfileMapper, CandidateProfile>
        implements CandidateProfileService {

    public static final String PROFILE_SCHEMA_VERSION = "v1";
    private static final int PROFILE_LOCK_NAMESPACE = 0x43505246;
    private static final long MESSAGE_FALLBACK_SECONDS = 300;
    private static final String MESSAGE_BIZ_PREFIX = "CANDIDATE_PROFILE:";

    private final ResumesMapper resumesMapper;
    private final LocalMessageApi localMessageApi;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public CandidateProfile ensureProfile(Long resumeId,
                                          Long candidateId,
                                          Long sourceApplicationId,
                                          Long sourceEnterpriseId,
                                          Long createdBy) {
        // 同一简历串行判断复用与创建，数据库唯一索引再做最终兜底。
        resumesMapper.lockResumes(
                PROFILE_LOCK_NAMESPACE, Long.hashCode(resumeId));
        Resumes resume = resumesMapper.selectOne(Wrappers.<Resumes>lambdaQuery()
                .select(Resumes::getId, Resumes::getResumeText)
                .eq(Resumes::getId, resumeId)
                .eq(Resumes::getUserId, candidateId));
        if (resume == null || StrUtil.isBlank(resume.getResumeText())) {
            throw new BusinessException(ErrorCode.RESUME_ANALYSIS_FAILED);
        }

        CandidateProfile completed = findReusableProfile(resumeId, candidateId);
        if (completed != null) {
            return completed;
        }
        CandidateProfile active = lambdaQuery()
                .eq(CandidateProfile::getResumeId, resumeId)
                .eq(CandidateProfile::getCandidateId, candidateId)
                .eq(CandidateProfile::getProfileSchemaVersion, PROFILE_SCHEMA_VERSION)
                .in(CandidateProfile::getStatus,
                        AiTaskStatus.PENDING, AiTaskStatus.PROCESSING)
                .orderByDesc(CandidateProfile::getCreatedAt)
                .last("LIMIT 1")
                .one();
        if (active != null) {
            return active;
        }

        OffsetDateTime now = OffsetDateTime.now();
        CandidateProfileCommand command =
                new CandidateProfileCommand(resumeId, candidateId);
        String payload;
        try {
            payload = objectMapper.writeValueAsString(command);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.CANDIDATE_PROFILE_GENERATION_FAILED);
        }
        MessageDTO message = MessageDTO.builder()
                .bizKey(MESSAGE_BIZ_PREFIX + resumeId + ":"
                        + PROFILE_SCHEMA_VERSION + ":" + IdWorker.getIdStr())
                .topic(MsgTopic.CANDIDATE_PROFILE_GENERATION)
                .schemaVersion(1)
                .payload(payload)
                .status(MsgStatus.PENDING)
                .priority(MsgPriority.HIGH)
                .maxRetries(5)
                .nextRetryAt(now.plusSeconds(MESSAGE_FALLBACK_SECONDS))
                .build();
        Long messageId = localMessageApi.saveInCurrentTransaction(message);

        CandidateProfile profile = CandidateProfile.builder()
                .id(messageId)
                .candidateId(candidateId)
                .resumeId(resumeId)
                .sourceApplicationId(sourceApplicationId)
                .sourceEnterpriseId(sourceEnterpriseId)
                .status(AiTaskStatus.PENDING)
                .profileSchemaVersion(PROFILE_SCHEMA_VERSION)
                .attemptCount(0)
                .createdBy(createdBy)
                .traceId(TraceUtil.getTraceId())
                .createdAt(now)
                .updatedAt(now)
                .build();
        if (baseMapper.insert(profile) != 1) {
            throw new BusinessException(ErrorCode.CANDIDATE_PROFILE_GENERATION_FAILED);
        }
        eventPublisher.publishEvent(
                new CandidateProfileCreatedEvent(messageId, resumeId, candidateId));
        return profile;
    }

    @Override
    public CandidateProfile findReusableProfile(Long resumeId, Long candidateId) {
        return lambdaQuery()
                .eq(CandidateProfile::getResumeId, resumeId)
                .eq(CandidateProfile::getCandidateId, candidateId)
                .eq(CandidateProfile::getProfileSchemaVersion, PROFILE_SCHEMA_VERSION)
                .eq(CandidateProfile::getStatus, AiTaskStatus.COMPLETED)
                .orderByDesc(CandidateProfile::getAnalyzedAt)
                .last("LIMIT 1")
                .one();
    }
}
