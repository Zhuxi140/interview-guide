package interview.matching.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import interview.api.infra.LocalMessageApi;
import interview.common.enums.AiTaskStatus;
import interview.common.enums.MessageHandleStatus;
import interview.common.enums.ScreeningRecommendation;
import interview.common.spi.MessageHandleResult;
import interview.common.util.TraceUtil;
import interview.matching.mapper.ApplicationAiScreeningMapper;
import interview.matching.model.bo.ApplicationScreeningExecutionBO;
import interview.matching.model.entity.ApplicationAiScreening;
import interview.matching.model.message.ApplicationScreeningCommand;
import interview.resume.mapper.CandidateProfileMapper;
import interview.resume.model.entity.CandidateProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 使用初筛结果表和本地消息维护 HR AI 初筛执行状态。
 */
@Service
@RequiredArgsConstructor
public class ApplicationScreeningStateService {

    static final int MAX_AI_ATTEMPTS = 2;
    static final long PROCESSING_TIMEOUT_SECONDS = 300;
    private static final long RETRY_DELAY_MINUTES = 1;

    private final ApplicationAiScreeningMapper screeningMapper;
    private final CandidateProfileMapper candidateProfileMapper;
    private final LocalMessageApi localMessageApi;

    /**
     * 检查并推进人才画像依赖。
     *
     * @param messageId 初筛任务 ID
     * @param command 初筛命令
     * @return 需要立即返回的处理结果；可以继续领取时返回 null
     */
    @Transactional
    public MessageHandleResult prepareProfile(
            Long messageId, ApplicationScreeningCommand command) {
        ApplicationAiScreening screening = findBoundScreening(messageId, command);
        if (screening == null) {
            return MessageHandleResult.permanentFailure(
                    "application screening binding mismatch");
        }
        if (screening.getStatus() == AiTaskStatus.COMPLETED) {
            return MessageHandleResult.ignored("application screening already completed");
        }
        if (screening.getStatus() == AiTaskStatus.FAILED) {
            return MessageHandleResult.permanentFailure(
                    "application screening already failed");
        }
        if (screening.getStatus() != AiTaskStatus.WAITING_PROFILE) {
            return null;
        }

        CandidateProfile profile = candidateProfileMapper.selectOne(
                Wrappers.<CandidateProfile>lambdaQuery()
                        .select(CandidateProfile::getStatus,
                                CandidateProfile::getDeadlineAt,
                                CandidateProfile::getFailureReason)
                        .eq(CandidateProfile::getId,
                                screening.getCandidateProfileId()));
        OffsetDateTime now = OffsetDateTime.now();
        if (profile == null || profile.getStatus() == AiTaskStatus.FAILED) {
            String reason = profile == null
                    ? "candidate profile missing"
                    : "candidate profile failed: " + profile.getFailureReason();
            screeningMapper.failWaitingScreening(
                    messageId, reason, TraceUtil.getTraceId(), now);
            return MessageHandleResult.permanentFailure(reason);
        }
        if (profile.getStatus() != AiTaskStatus.COMPLETED) {
            OffsetDateTime retryAt = profile.getDeadlineAt() != null
                    && profile.getDeadlineAt().isAfter(now)
                    ? profile.getDeadlineAt() : now.plusSeconds(30);
            return MessageHandleResult.retry(retryAt, "waiting candidate profile");
        }

        int updated = screeningMapper.update(null,
                Wrappers.<ApplicationAiScreening>lambdaUpdate()
                        .eq(ApplicationAiScreening::getId, messageId)
                        .eq(ApplicationAiScreening::getStatus,
                                AiTaskStatus.WAITING_PROFILE)
                        .set(ApplicationAiScreening::getStatus,
                                AiTaskStatus.PENDING)
                        .set(ApplicationAiScreening::getTraceId,
                                TraceUtil.getTraceId())
                        .set(ApplicationAiScreening::getUpdatedAt, now));
        return updated == 1
                ? null
                : MessageHandleResult.retry(
                        now.plusSeconds(5), "screening profile transition lost");
    }

    /**
     * 原子领取一次 HR AI 初筛执行权。
     *
     * @param messageId 初筛任务 ID
     * @return 执行上下文；未领取到返回 null
     */
    @Transactional
    public ApplicationScreeningExecutionBO claim(Long messageId) {
        OffsetDateTime now = OffsetDateTime.now();
        return screeningMapper.claimScreening(
                messageId, MAX_AI_ATTEMPTS, now,
                now.plusSeconds(PROCESSING_TIMEOUT_SECONDS),
                TraceUtil.getTraceId());
    }

    /**
     * 解释未领取到初筛任务时的当前状态。
     *
     * @param messageId 初筛任务 ID
     * @param command 初筛命令
     * @return 消息处理结果
     */
    @Transactional
    public MessageHandleResult resolveUnclaimed(
            Long messageId, ApplicationScreeningCommand command) {
        ApplicationAiScreening screening = findBoundScreening(messageId, command);
        if (screening == null) {
            return MessageHandleResult.permanentFailure(
                    "application screening binding mismatch");
        }
        if (screening.getStatus() == AiTaskStatus.COMPLETED) {
            return MessageHandleResult.ignored("application screening already completed");
        }
        if (screening.getStatus() == AiTaskStatus.FAILED) {
            return MessageHandleResult.permanentFailure(
                    "application screening already failed");
        }
        OffsetDateTime now = OffsetDateTime.now();
        boolean leaseExpired = screening.getDeadlineAt() == null
                || !screening.getDeadlineAt().isAfter(now);
        int attempts = screening.getAttemptCount() == null
                ? 0 : screening.getAttemptCount();
        if (attempts >= MAX_AI_ATTEMPTS && leaseExpired) {
            int updated = screeningMapper.failExhaustedScreening(
                    messageId, MAX_AI_ATTEMPTS,
                    "application screening attempts exhausted",
                    TraceUtil.getTraceId(), now);
            if (updated == 1) {
                return MessageHandleResult.permanentFailure(
                        "application screening attempts exhausted");
            }
        }
        if (screening.getStatus() == AiTaskStatus.PROCESSING && !leaseExpired) {
            return MessageHandleResult.retry(
                    screening.getDeadlineAt(), "application screening is processing");
        }
        return MessageHandleResult.retry(
                now.plusSeconds(5), "application screening claim lost");
    }

    /**
     * 按执行栅栏保存 HR AI 初筛结果。
     *
     * @param execution 执行上下文
     * @param overallScore 总体匹配分
     * @param dimensionsJson 维度匹配 JSON
     * @param recommendation 服务端建议
     * @param snapshotJson 模型配置快照 JSON
     * @return 是否成功提交
     */
    @Transactional
    public boolean complete(ApplicationScreeningExecutionBO execution,
                            Integer overallScore,
                            String dimensionsJson,
                            ScreeningRecommendation recommendation,
                            String snapshotJson) {
        return screeningMapper.completeScreening(
                execution.screeningId(), execution.attemptCount(), overallScore,
                dimensionsJson, recommendation.name(), snapshotJson,
                TraceUtil.getTraceId(), OffsetDateTime.now()) == 1;
    }

    /**
     * 记录本次初筛失败并决定是否继续重试。
     *
     * @param execution 执行上下文
     * @param error 错误摘要
     * @return 消息处理结果
     */
    @Transactional
    public MessageHandleResult fail(
            ApplicationScreeningExecutionBO execution, String error) {
        OffsetDateTime now = OffsetDateTime.now();
        if (execution.attemptCount() >= MAX_AI_ATTEMPTS) {
            int updated = screeningMapper.failScreening(
                    execution.screeningId(), execution.attemptCount(), error,
                    TraceUtil.getTraceId(), now);
            return updated == 1
                    ? MessageHandleResult.permanentFailure(error)
                    : MessageHandleResult.ignored("application screening lease lost");
        }
        int updated = screeningMapper.releaseScreeningForRetry(
                execution.screeningId(), execution.attemptCount(), error,
                TraceUtil.getTraceId(), now);
        return updated == 1
                ? MessageHandleResult.retry(now.plusMinutes(RETRY_DELAY_MINUTES), error)
                : MessageHandleResult.ignored("application screening lease lost");
    }

    /**
     * 将直接异步结果同步到初筛兜底消息。
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
        localMessageApi.schedulePending(messageId, result.retryAt() == null
                ? OffsetDateTime.now() : result.retryAt());
    }

    private ApplicationAiScreening findBoundScreening(
            Long messageId, ApplicationScreeningCommand command) {
        return screeningMapper.selectOne(
                Wrappers.<ApplicationAiScreening>lambdaQuery()
                        .select(ApplicationAiScreening::getStatus,
                                ApplicationAiScreening::getCandidateProfileId,
                                ApplicationAiScreening::getAttemptCount,
                                ApplicationAiScreening::getDeadlineAt)
                        .eq(ApplicationAiScreening::getId, messageId)
                        .eq(ApplicationAiScreening::getApplicationId,
                                command.applicationId()));
    }
}
