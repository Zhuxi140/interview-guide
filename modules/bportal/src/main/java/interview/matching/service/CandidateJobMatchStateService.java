package interview.matching.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import interview.api.infra.LocalMessageApi;
import interview.common.enums.AiTaskStatus;
import interview.common.enums.MessageHandleStatus;
import interview.common.spi.MessageHandleResult;
import interview.common.util.TraceUtil;
import interview.matching.mapper.CandidateJobMatchAnalysisMapper;
import interview.matching.model.bo.CandidateJobMatchExecutionBO;
import interview.matching.model.entity.CandidateJobMatchAnalysis;
import interview.matching.model.message.CandidateJobMatchCommand;
import interview.resume.mapper.CandidateProfileMapper;
import interview.resume.model.entity.CandidateProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 使用预测结果表和本地消息维护候选人岗位预测执行状态。
 */
@Service
@RequiredArgsConstructor
public class CandidateJobMatchStateService {

    static final int MAX_AI_ATTEMPTS = 2;
    static final long PROCESSING_TIMEOUT_SECONDS = 300;
    private static final long RETRY_DELAY_MINUTES = 1;

    private final CandidateJobMatchAnalysisMapper analysisMapper;
    private final CandidateProfileMapper candidateProfileMapper;
    private final LocalMessageApi localMessageApi;

    /**
     * 检查并推进人才画像依赖。
     *
     * @param messageId 预测任务 ID
     * @param command 预测命令
     * @return 需要立即返回的处理结果；可以继续领取时返回 null
     */
    @Transactional
    public MessageHandleResult prepareProfile(
            Long messageId, CandidateJobMatchCommand command) {
        CandidateJobMatchAnalysis analysis = findBoundAnalysis(messageId, command);
        if (analysis == null) {
            return MessageHandleResult.permanentFailure(
                    "candidate job match binding mismatch");
        }
        if (analysis.getStatus() == AiTaskStatus.COMPLETED) {
            return MessageHandleResult.ignored("candidate job match already completed");
        }
        if (analysis.getStatus() == AiTaskStatus.FAILED) {
            return MessageHandleResult.permanentFailure(
                    "candidate job match already failed");
        }
        if (analysis.getStatus() != AiTaskStatus.WAITING_PROFILE) {
            return null;
        }

        CandidateProfile profile = candidateProfileMapper.selectOne(
                Wrappers.<CandidateProfile>lambdaQuery()
                        .select(CandidateProfile::getStatus,
                                CandidateProfile::getDeadlineAt,
                                CandidateProfile::getFailureReason)
                        .eq(CandidateProfile::getId,
                                analysis.getCandidateProfileId()));
        OffsetDateTime now = OffsetDateTime.now();
        if (profile == null || profile.getStatus() == AiTaskStatus.FAILED) {
            String reason = profile == null
                    ? "candidate profile missing"
                    : "candidate profile failed: " + profile.getFailureReason();
            analysisMapper.failWaitingAnalysis(
                    messageId, reason, TraceUtil.getTraceId(), now);
            return MessageHandleResult.permanentFailure(reason);
        }
        if (profile.getStatus() != AiTaskStatus.COMPLETED) {
            OffsetDateTime retryAt = profile.getDeadlineAt() != null
                    && profile.getDeadlineAt().isAfter(now)
                    ? profile.getDeadlineAt() : now.plusSeconds(30);
            return MessageHandleResult.retry(retryAt, "waiting candidate profile");
        }

        int updated = analysisMapper.update(null,
                Wrappers.<CandidateJobMatchAnalysis>lambdaUpdate()
                        .eq(CandidateJobMatchAnalysis::getId, messageId)
                        .eq(CandidateJobMatchAnalysis::getStatus,
                                AiTaskStatus.WAITING_PROFILE)
                        .set(CandidateJobMatchAnalysis::getStatus,
                                AiTaskStatus.PENDING)
                        .set(CandidateJobMatchAnalysis::getTraceId,
                                TraceUtil.getTraceId())
                        .set(CandidateJobMatchAnalysis::getUpdatedAt, now));
        return updated == 1
                ? null
                : MessageHandleResult.retry(
                        now.plusSeconds(5), "job match profile transition lost");
    }

    /**
     * 原子领取一次候选人岗位预测执行权。
     *
     * @param messageId 预测任务 ID
     * @return 执行上下文；未领取到返回 null
     */
    @Transactional
    public CandidateJobMatchExecutionBO claim(Long messageId) {
        OffsetDateTime now = OffsetDateTime.now();
        return analysisMapper.claimAnalysis(
                messageId, MAX_AI_ATTEMPTS, now,
                now.plusSeconds(PROCESSING_TIMEOUT_SECONDS),
                TraceUtil.getTraceId());
    }

    /**
     * 解释未领取到预测任务时的当前状态。
     *
     * @param messageId 预测任务 ID
     * @param command 预测命令
     * @return 消息处理结果
     */
    @Transactional
    public MessageHandleResult resolveUnclaimed(
            Long messageId, CandidateJobMatchCommand command) {
        CandidateJobMatchAnalysis analysis = findBoundAnalysis(messageId, command);
        if (analysis == null) {
            return MessageHandleResult.permanentFailure(
                    "candidate job match binding mismatch");
        }
        if (analysis.getStatus() == AiTaskStatus.COMPLETED) {
            return MessageHandleResult.ignored("candidate job match already completed");
        }
        if (analysis.getStatus() == AiTaskStatus.FAILED) {
            return MessageHandleResult.permanentFailure(
                    "candidate job match already failed");
        }
        OffsetDateTime now = OffsetDateTime.now();
        boolean leaseExpired = analysis.getDeadlineAt() == null
                || !analysis.getDeadlineAt().isAfter(now);
        int attempts = analysis.getAttemptCount() == null
                ? 0 : analysis.getAttemptCount();
        if (attempts >= MAX_AI_ATTEMPTS && leaseExpired) {
            int updated = analysisMapper.failExhaustedAnalysis(
                    messageId, MAX_AI_ATTEMPTS,
                    "candidate job match attempts exhausted",
                    TraceUtil.getTraceId(), now);
            if (updated == 1) {
                return MessageHandleResult.permanentFailure(
                        "candidate job match attempts exhausted");
            }
        }
        if (analysis.getStatus() == AiTaskStatus.PROCESSING && !leaseExpired) {
            return MessageHandleResult.retry(
                    analysis.getDeadlineAt(), "candidate job match is processing");
        }
        return MessageHandleResult.retry(
                now.plusSeconds(5), "candidate job match claim lost");
    }

    /**
     * 按执行栅栏保存候选人岗位预测结果。
     *
     * @param execution 执行上下文
     * @param matchScore 适配分
     * @param passProbability 通过率预测
     * @param strengthsJson 优势 JSON
     * @param gapsJson 差距 JSON
     * @param snapshotJson 模型配置快照 JSON
     * @return 是否成功提交
     */
    @Transactional
    public boolean complete(CandidateJobMatchExecutionBO execution,
                            Integer matchScore,
                            Integer passProbability,
                            String strengthsJson,
                            String gapsJson,
                            String snapshotJson) {
        return analysisMapper.completeAnalysis(
                execution.analysisId(), execution.attemptCount(), matchScore,
                passProbability, strengthsJson, gapsJson, snapshotJson,
                TraceUtil.getTraceId(), OffsetDateTime.now()) == 1;
    }

    /**
     * 记录本次岗位预测失败并决定是否继续重试。
     *
     * @param execution 执行上下文
     * @param error 错误摘要
     * @return 消息处理结果
     */
    @Transactional
    public MessageHandleResult fail(
            CandidateJobMatchExecutionBO execution, String error) {
        OffsetDateTime now = OffsetDateTime.now();
        if (execution.attemptCount() >= MAX_AI_ATTEMPTS) {
            int updated = analysisMapper.failAnalysis(
                    execution.analysisId(), execution.attemptCount(), error,
                    TraceUtil.getTraceId(), now);
            return updated == 1
                    ? MessageHandleResult.permanentFailure(error)
                    : MessageHandleResult.ignored("candidate job match lease lost");
        }
        int updated = analysisMapper.releaseAnalysisForRetry(
                execution.analysisId(), execution.attemptCount(), error,
                TraceUtil.getTraceId(), now);
        return updated == 1
                ? MessageHandleResult.retry(now.plusMinutes(RETRY_DELAY_MINUTES), error)
                : MessageHandleResult.ignored("candidate job match lease lost");
    }

    /**
     * 将直接异步结果同步到候选人预测兜底消息。
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

    private CandidateJobMatchAnalysis findBoundAnalysis(
            Long messageId, CandidateJobMatchCommand command) {
        return analysisMapper.selectOne(
                Wrappers.<CandidateJobMatchAnalysis>lambdaQuery()
                        .select(CandidateJobMatchAnalysis::getStatus,
                                CandidateJobMatchAnalysis::getCandidateProfileId,
                                CandidateJobMatchAnalysis::getAttemptCount,
                                CandidateJobMatchAnalysis::getDeadlineAt)
                        .eq(CandidateJobMatchAnalysis::getId, messageId)
                        .eq(CandidateJobMatchAnalysis::getApplicationId,
                                command.applicationId()));
    }
}
