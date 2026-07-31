package interview.resume.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import interview.api.aicore.dto.AiCandidateDimensionScore;
import interview.api.infra.LocalMessageApi;
import interview.common.enums.AiTaskStatus;
import interview.common.enums.ErrorCode;
import interview.common.enums.MessageHandleStatus;
import interview.common.exception.BusinessException;
import interview.common.spi.MessageHandleResult;
import interview.common.util.TraceUtil;
import interview.resume.mapper.CandidateProfileMapper;
import interview.resume.mapper.CandidateSkillScoresMapper;
import interview.resume.model.bo.CandidateProfileExecutionBO;
import interview.resume.model.entity.CandidateProfile;
import interview.resume.model.entity.CandidateSkillScores;
import interview.resume.model.message.CandidateProfileCommand;
import interview.resume.event.CandidateProfileCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 使用画像结果表和本地消息维护人才画像执行状态。
 */
@Service
@RequiredArgsConstructor
public class CandidateProfileStateService {

    static final int MAX_AI_ATTEMPTS = 2;
    static final long PROCESSING_TIMEOUT_SECONDS = 300;
    private static final long RETRY_DELAY_MINUTES = 1;

    private final CandidateProfileMapper candidateProfileMapper;
    private final CandidateSkillScoresMapper candidateSkillScoresMapper;
    private final LocalMessageApi localMessageApi;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 原子领取一次人才画像执行权。
     *
     * @param messageId 消息 ID
     * @param command 画像命令
     * @return 执行上下文；未领取到返回 null
     */
    @Transactional
    public CandidateProfileExecutionBO claim(
            Long messageId, CandidateProfileCommand command) {
        OffsetDateTime now = OffsetDateTime.now();
        return candidateProfileMapper.claimProfile(
                messageId, command.resumeId(), command.candidateId(),
                MAX_AI_ATTEMPTS, now,
                now.plusSeconds(PROCESSING_TIMEOUT_SECONDS),
                TraceUtil.getTraceId());
    }

    /**
     * 解释未领取到画像任务时的当前状态。
     *
     * @param messageId 消息 ID
     * @param command 画像命令
     * @return 消息处理结果
     */
    @Transactional
    public MessageHandleResult resolveUnclaimed(
            Long messageId, CandidateProfileCommand command) {
        CandidateProfile profile = candidateProfileMapper.selectOne(
                Wrappers.<CandidateProfile>lambdaQuery()
                        .select(CandidateProfile::getStatus,
                                CandidateProfile::getAttemptCount,
                                CandidateProfile::getDeadlineAt)
                        .eq(CandidateProfile::getId, messageId)
                        .eq(CandidateProfile::getResumeId, command.resumeId())
                        .eq(CandidateProfile::getCandidateId, command.candidateId()));
        if (profile == null) {
            return MessageHandleResult.permanentFailure(
                    "candidate profile binding mismatch");
        }
        if (profile.getStatus() == AiTaskStatus.COMPLETED) {
            return MessageHandleResult.ignored("candidate profile already completed");
        }
        if (profile.getStatus() == AiTaskStatus.FAILED) {
            return MessageHandleResult.permanentFailure(
                    "candidate profile already failed");
        }

        OffsetDateTime now = OffsetDateTime.now();
        boolean leaseExpired = profile.getDeadlineAt() == null
                || !profile.getDeadlineAt().isAfter(now);
        int attempts = profile.getAttemptCount() == null
                ? 0 : profile.getAttemptCount();
        if (attempts >= MAX_AI_ATTEMPTS && leaseExpired) {
            int updated = candidateProfileMapper.failExhaustedProfile(
                    messageId, MAX_AI_ATTEMPTS,
                    "candidate profile attempts exhausted",
                    TraceUtil.getTraceId(), now);
            if (updated == 1) {
                return MessageHandleResult.permanentFailure(
                        "candidate profile attempts exhausted");
            }
        }
        if (profile.getStatus() == AiTaskStatus.PROCESSING && !leaseExpired) {
            return MessageHandleResult.retry(
                    profile.getDeadlineAt(), "candidate profile is processing");
        }
        return MessageHandleResult.retry(
                now.plusSeconds(5), "candidate profile claim lost");
    }

    /**
     * 按执行栅栏保存画像头信息和维度分数。
     *
     * @param execution 执行上下文
     * @param summaryJson 摘要 JSON
     * @param snapshotJson 模型配置快照 JSON
     * @param dimensions 固定维度评分
     * @return 是否成功提交
     */
    @Transactional
    public boolean complete(CandidateProfileExecutionBO execution,
                            String summaryJson,
                            String snapshotJson,
                            List<AiCandidateDimensionScore> dimensions) {
        OffsetDateTime now = OffsetDateTime.now();
        int updated = candidateProfileMapper.completeProfile(
                execution.profileId(), execution.attemptCount(),
                summaryJson, snapshotJson, TraceUtil.getTraceId(), now);
        if (updated != 1) {
            return false;
        }
        try {
            for (AiCandidateDimensionScore dimension : dimensions) {
                CandidateSkillScores score = CandidateSkillScores.builder()
                        .candidateProfileId(execution.profileId())
                        .dimensionCode(dimension.dimensionCode())
                        .score(dimension.score())
                        .aiJustification(dimension.justification())
                        .evidenceJson(objectMapper.writeValueAsString(
                                dimension.evidence()))
                        .createdAt(now)
                        .build();
                candidateSkillScoresMapper.insert(score);
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }
        eventPublisher.publishEvent(
                new CandidateProfileCompletedEvent(execution.profileId()));
        return true;
    }

    /**
     * 记录本次画像执行失败并决定是否继续重试。
     *
     * @param execution 执行上下文
     * @param error 错误摘要
     * @return 消息处理结果
     */
    @Transactional
    public MessageHandleResult fail(
            CandidateProfileExecutionBO execution, String error) {
        OffsetDateTime now = OffsetDateTime.now();
        if (execution.attemptCount() >= MAX_AI_ATTEMPTS) {
            int updated = candidateProfileMapper.failProfile(
                    execution.profileId(), execution.attemptCount(), error,
                    TraceUtil.getTraceId(), now);
            return updated == 1
                    ? MessageHandleResult.permanentFailure(error)
                    : MessageHandleResult.ignored("candidate profile lease lost");
        }
        int updated = candidateProfileMapper.releaseProfileForRetry(
                execution.profileId(), execution.attemptCount(), error,
                TraceUtil.getTraceId(), now);
        return updated == 1
                ? MessageHandleResult.retry(now.plusMinutes(RETRY_DELAY_MINUTES), error)
                : MessageHandleResult.ignored("candidate profile lease lost");
    }

    /**
     * 将直接异步结果同步到画像兜底消息。
     *
     * @param messageId 消息 ID
     * @param result 执行结果
     */
    @Transactional
    public void applyDirectResult(Long messageId, MessageHandleResult result) {
        if (result.status() == MessageHandleStatus.SUCCESS
                || result.status() == MessageHandleStatus.IGNORED) {
            localMessageApi.ignorePending(messageId);
            return;
        }
        OffsetDateTime executeAt = result.retryAt() == null
                ? OffsetDateTime.now() : result.retryAt();
        localMessageApi.schedulePending(messageId, executeAt);
    }
}
