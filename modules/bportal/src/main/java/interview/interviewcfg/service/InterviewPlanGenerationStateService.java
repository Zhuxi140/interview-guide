package interview.interviewcfg.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import interview.api.infra.LocalMessageApi;
import interview.common.enums.MessageHandleStatus;
import interview.common.spi.MessageHandleResult;
import interview.common.util.TraceUtil;
import interview.interviewcfg.mapper.InterviewPlanDraftMapper;
import interview.interviewcfg.model.bo.InterviewPlanGenerationExecutionBO;
import interview.interviewcfg.model.entity.InterviewPlanDraft;
import interview.interviewcfg.model.enums.InterviewPlanDraftStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * 使用草案状态和本地消息维护 Agent 生成执行状态（消息租约模式）。
 */
@Service
@RequiredArgsConstructor
public class InterviewPlanGenerationStateService {

    static final int MAX_AI_ATTEMPTS = 2;
    static final long PROCESSING_TIMEOUT_SECONDS = 300;
    private static final long RETRY_DELAY_MINUTES = 1;

    private final InterviewPlanDraftMapper interviewPlanDraftMapper;
    private final LocalMessageApi localMessageApi;

    /**
     * 原子领取一次 Agent 草案生成执行权。
     *
     * @param messageId 生成消息 ID
     * @param draftId 草案 ID
     * @return 执行上下文；未领取到返回 null
     */
    @Transactional
    public InterviewPlanGenerationExecutionBO claim(Long messageId, Long draftId) {
        OffsetDateTime now = OffsetDateTime.now();
        return interviewPlanDraftMapper.claimGeneration(
                draftId, messageId, MAX_AI_ATTEMPTS, now,
                now.plusSeconds(PROCESSING_TIMEOUT_SECONDS), TraceUtil.getTraceId());
    }

    /**
     * 解释未领取到任务时的当前业务状态。
     *
     * @param messageId 生成消息 ID
     * @param draftId 草案 ID
     * @return 消息处理结果
     */
    @Transactional
    public MessageHandleResult resolveUnclaimed(Long messageId, Long draftId) {
        InterviewPlanDraft draft = findDraft(draftId);
        if (draft == null || !Objects.equals(draft.getGenerationMessageId(), messageId)) {
            return MessageHandleResult.permanentFailure(
                    "interview plan generation binding mismatch");
        }
        if (draft.getStatus() == InterviewPlanDraftStatus.READY) {
            return MessageHandleResult.ignored("interview plan already ready");
        }
        if (draft.getStatus() == InterviewPlanDraftStatus.FAILED) {
            return MessageHandleResult.permanentFailure(
                    "interview plan generation already failed");
        }

        OffsetDateTime now = OffsetDateTime.now();
        int attempts = draft.getAttemptCount() == null ? 0 : draft.getAttemptCount();
        boolean leaseExpired = draft.getGenerationDeadlineAt() == null
                || !draft.getGenerationDeadlineAt().isAfter(now);
        if (attempts >= MAX_AI_ATTEMPTS
                && (draft.getStatus() == InterviewPlanDraftStatus.PENDING
                || (draft.getStatus() == InterviewPlanDraftStatus.PROCESSING
                && leaseExpired))) {
            int updated = interviewPlanDraftMapper.failExhaustedGeneration(
                    draftId, messageId, MAX_AI_ATTEMPTS,
                    "interview plan generation attempts exhausted",
                    TraceUtil.getTraceId(), now);
            if (updated == 1) {
                return MessageHandleResult.permanentFailure(
                        "interview plan generation attempts exhausted");
            }
        }
        if (draft.getStatus() == InterviewPlanDraftStatus.PROCESSING
                && !leaseExpired) {
            return MessageHandleResult.retry(
                    draft.getGenerationDeadlineAt(),
                    "interview plan generation is processing");
        }
        if (draft.getStatus() == InterviewPlanDraftStatus.PENDING
                || draft.getStatus() == InterviewPlanDraftStatus.PROCESSING) {
            return MessageHandleResult.retry(
                    now.plusSeconds(5), "interview plan generation claim lost");
        }
        return MessageHandleResult.permanentFailure(
                "invalid interview plan generation status: " + draft.getStatus());
    }

    /**
     * 按执行栅栏保存生成结果并将草案置为 READY。
     *
     * @param execution 执行上下文
     * @param planJson 生成计划
     * @param snapshotJson LLM 配置快照 JSON
     * @return 是否成功提交
     */
    @Transactional
    public boolean complete(InterviewPlanGenerationExecutionBO execution, String planJson,
                            String snapshotJson) {
        OffsetDateTime now = OffsetDateTime.now();
        int updated = interviewPlanDraftMapper.completeGeneration(
                execution.draftId(), execution.messageId(), execution.attemptCount(),
                planJson, snapshotJson, now.plusDays(InterviewPlanDraftService.DRAFT_VALIDITY_DAYS),
                TraceUtil.getTraceId(), now);
        return updated == 1;
    }

    /**
     * 记录本次执行失败并决定是否继续重试。
     *
     * @param execution 执行上下文
     * @param error 错误摘要
     * @return 消息处理结果
     */
    @Transactional
    public MessageHandleResult fail(
            InterviewPlanGenerationExecutionBO execution, String error) {
        OffsetDateTime now = OffsetDateTime.now();
        if (execution.attemptCount() >= MAX_AI_ATTEMPTS) {
            int updated = interviewPlanDraftMapper.failGeneration(
                    execution.draftId(), execution.messageId(),
                    execution.attemptCount(), error, TraceUtil.getTraceId(), now);
            return updated == 1
                    ? MessageHandleResult.permanentFailure(error)
                    : MessageHandleResult.ignored(
                            "interview plan generation lease lost");
        }

        int updated = interviewPlanDraftMapper.releaseGenerationForRetry(
                execution.draftId(), execution.messageId(),
                execution.attemptCount(), TraceUtil.getTraceId(), now);
        return updated == 1
                ? MessageHandleResult.retry(
                        now.plusMinutes(RETRY_DELAY_MINUTES), error)
                : MessageHandleResult.ignored(
                        "interview plan generation lease lost");
    }

    /**
     * 将直接异步执行结果同步到待执行兜底消息。
     *
     * @param messageId 生成消息 ID
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

    private InterviewPlanDraft findDraft(Long draftId) {
        return interviewPlanDraftMapper.selectOne(Wrappers.<InterviewPlanDraft>lambdaQuery()
                .select(InterviewPlanDraft::getGenerationMessageId,
                        InterviewPlanDraft::getStatus,
                        InterviewPlanDraft::getAttemptCount,
                        InterviewPlanDraft::getGenerationDeadlineAt)
                .eq(InterviewPlanDraft::getId, draftId));
    }
}