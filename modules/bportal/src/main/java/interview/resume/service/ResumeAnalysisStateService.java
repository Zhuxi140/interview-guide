package interview.resume.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import interview.api.infra.LocalMessageApi;
import interview.api.infra.dto.MessageDTO;
import interview.common.enums.ErrorCode;
import interview.common.enums.MessageHandleStatus;
import interview.common.enums.MsgPriority;
import interview.common.enums.MsgStatus;
import interview.common.enums.MsgTopic;
import interview.common.exception.BusinessException;
import interview.common.spi.MessageHandleResult;
import interview.common.util.TraceUtil;
import interview.resume.event.ResumeAnalysisCreatedEvent;
import interview.resume.mapper.ResumeAnalysesMapper;
import interview.resume.mapper.ResumesMapper;
import interview.resume.model.bo.ResumeAnalysisExecutionBO;
import interview.resume.model.entity.ResumeAnalyses;
import interview.resume.model.entity.Resumes;
import interview.resume.model.enums.AnalyzeStatus;
import interview.resume.model.message.ResumeAnalysisCommand;
import interview.resume.model.vo.ResumeAnalyzeTriggerVO;
import interview.resume.support.ResumeLockKey;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * 使用简历状态和本地消息维护 AI 分析执行状态。
 */
@Service
@RequiredArgsConstructor
public class ResumeAnalysisStateService {

    static final int MAX_AI_ATTEMPTS = 2;
    static final long PROCESSING_TIMEOUT_SECONDS = 300;
    private static final long RETRY_DELAY_MINUTES = 1;
    private static final String MESSAGE_BIZ_PREFIX = "RESUME_ANALYSIS:";

    private final ResumesMapper resumesMapper;
    private final ResumeAnalysesMapper resumeAnalysesMapper;
    private final LocalMessageApi localMessageApi;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * 幂等受理简历 AI 分析请求。
     *
     * @param userId 当前用户 ID
     * @param resumeId 简历 ID
     * @param idempotencyKey 幂等键
     * @return 任务受理结果
     */
    @Transactional
    public ResumeAnalyzeTriggerVO accept(Long userId, Long resumeId, String idempotencyKey) {
        // 用户级事务锁避免同一用户并发创建重复分析消息。
        resumesMapper.lockResumes(ResumeLockKey.NAMESPACE, ResumeLockKey.ownerSlot(userId));

        Resumes resume = resumesMapper.selectOne(Wrappers.<Resumes>lambdaQuery()
                .select(Resumes::getId, Resumes::getUserId, Resumes::getResumeText,
                        Resumes::getAnalyzeStatus, Resumes::getAnalysisMessageId,
                        Resumes::getAnalysisIdempotencyKeyHash)
                .eq(Resumes::getId, resumeId)
                .eq(Resumes::getUserId, userId));
        if (resume == null) {
            throw new BusinessException(ErrorCode.RESUME_NOT_FOUND);
        }
        if (StrUtil.isBlank(resume.getResumeText())) {
            throw new BusinessException(ErrorCode.RESUME_ANALYSIS_FAILED);
        }

        String keyHash = DigestUtil.sha256Hex(
                userId + ":" + resumeId + ":" + idempotencyKey);
        String bizKey = MESSAGE_BIZ_PREFIX + keyHash;

        // 当前请求重放直接返回当前业务状态。
        if (Objects.equals(keyHash, resume.getAnalysisIdempotencyKeyHash())
                && resume.getAnalysisMessageId() != null) {
            return new ResumeAnalyzeTriggerVO(
                    resume.getAnalysisMessageId(), resumeId, resume.getAnalyzeStatus());
        }

        // 已被后续失败任务替换的旧幂等键仍返回原消息 ID。
        Long historicalMessageId =
                localMessageApi.findIdByBizKey(MsgTopic.RESUME_AI_PARSER, bizKey);
        if (historicalMessageId != null) {
            return new ResumeAnalyzeTriggerVO(
                    historicalMessageId, resumeId, AnalyzeStatus.FAILED);
        }

        boolean active = resume.getAnalysisMessageId() != null
                && (resume.getAnalyzeStatus() == AnalyzeStatus.PENDING
                || resume.getAnalyzeStatus() == AnalyzeStatus.PROCESSING);
        if (active || (resume.getAnalyzeStatus() != AnalyzeStatus.PENDING
                && resume.getAnalyzeStatus() != AnalyzeStatus.FAILED)) {
            throw new BusinessException(ErrorCode.RESUME_ANALYZE_STATUS_ERROR);
        }

        OffsetDateTime now = OffsetDateTime.now();
        ResumeAnalysisCommand command = new ResumeAnalysisCommand(resumeId, userId);
        String payload;
        try {
            payload = objectMapper.writeValueAsString(command);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.RESUME_ANALYSIS_FAILED);
        }

        // 延迟五分钟开放消息兜底，正常路径在事务提交后立即异步执行。
        MessageDTO message = MessageDTO.builder()
                .bizKey(bizKey)
                .topic(MsgTopic.RESUME_AI_PARSER)
                .schemaVersion(1)
                .payload(payload)
                .status(MsgStatus.PENDING)
                .priority(MsgPriority.HIGH)
                .maxRetries(5)
                .nextRetryAt(now.plusSeconds(PROCESSING_TIMEOUT_SECONDS))
                .build();
        Long messageId = localMessageApi.saveInCurrentTransaction(message);
        int updated = resumesMapper.bindAnalysisRequest(
                resumeId, userId, resume.getAnalyzeStatus(),
                resume.getAnalysisMessageId(), messageId, keyHash,
                TraceUtil.getTraceId(), now);
        if (updated != 1) {
            throw new BusinessException(ErrorCode.RESUME_ANALYZE_STATUS_ERROR);
        }

        eventPublisher.publishEvent(
                new ResumeAnalysisCreatedEvent(messageId, resumeId, userId));
        return new ResumeAnalyzeTriggerVO(messageId, resumeId, AnalyzeStatus.PENDING);
    }

    /**
     * 原子领取一次简历分析执行权。
     *
     * @param messageId 消息 ID
     * @param command 分析命令
     * @return 执行上下文；未领取到返回 null
     */
    @Transactional
    public ResumeAnalysisExecutionBO claim(Long messageId, ResumeAnalysisCommand command) {
        OffsetDateTime now = OffsetDateTime.now();
        return resumesMapper.claimAnalysis(
                command.resumeId(), command.userId(), messageId,
                MAX_AI_ATTEMPTS, now,
                now.plusSeconds(PROCESSING_TIMEOUT_SECONDS),
                TraceUtil.getTraceId());
    }

    /**
     * 解释未领取到任务时的当前业务状态。
     *
     * @param messageId 消息 ID
     * @param command 分析命令
     * @return 消息处理结果
     */
    @Transactional
    public MessageHandleResult resolveUnclaimed(
            Long messageId, ResumeAnalysisCommand command) {
        Resumes resume = findBoundResume(command);
        if (resume == null || !Objects.equals(resume.getAnalysisMessageId(), messageId)) {
            return MessageHandleResult.permanentFailure(
                    "resume analysis binding mismatch");
        }
        if (resume.getAnalyzeStatus() == AnalyzeStatus.COMPLETED) {
            return MessageHandleResult.ignored("resume analysis already completed");
        }
        if (resume.getAnalyzeStatus() == AnalyzeStatus.FAILED) {
            return MessageHandleResult.permanentFailure(
                    "resume analysis already failed");
        }

        OffsetDateTime now = OffsetDateTime.now();
        int attempts = resume.getAnalysisAttemptCount() == null
                ? 0 : resume.getAnalysisAttemptCount();
        boolean leaseExpired = resume.getAnalysisDeadlineAt() == null
                || !resume.getAnalysisDeadlineAt().isAfter(now);
        if (attempts >= MAX_AI_ATTEMPTS
                && (resume.getAnalyzeStatus() == AnalyzeStatus.PENDING
                || (resume.getAnalyzeStatus() == AnalyzeStatus.PROCESSING
                && leaseExpired))) {
            int updated = resumesMapper.failExhaustedAnalysis(
                    command.resumeId(), command.userId(), messageId,
                    MAX_AI_ATTEMPTS, TraceUtil.getTraceId(), now);
            if (updated == 1) {
                return MessageHandleResult.permanentFailure(
                        "resume analysis attempts exhausted");
            }
        }
        if (resume.getAnalyzeStatus() == AnalyzeStatus.PROCESSING
                && !leaseExpired) {
            return MessageHandleResult.retry(
                    resume.getAnalysisDeadlineAt(), "resume analysis is processing");
        }
        if (resume.getAnalyzeStatus() == AnalyzeStatus.PENDING
                || resume.getAnalyzeStatus() == AnalyzeStatus.PROCESSING) {
            return MessageHandleResult.retry(
                    now.plusSeconds(5), "resume analysis claim lost");
        }
        return MessageHandleResult.permanentFailure(
                "invalid resume analysis status: " + resume.getAnalyzeStatus());
    }

    /**
     * 按执行栅栏保存结果并完成简历分析。
     *
     * @param execution 执行上下文
     * @param overallScore 综合评分
     * @param strengthsJson 优势 JSON
     * @param suggestionsJson 建议 JSON
     * @param snapshotJson 配置快照 JSON
     * @return 是否成功提交
     */
    @Transactional
    public boolean complete(ResumeAnalysisExecutionBO execution,
                            Integer overallScore,
                            String strengthsJson,
                            String suggestionsJson,
                            String snapshotJson) {
        OffsetDateTime now = OffsetDateTime.now();
        int updated = resumesMapper.completeAnalysis(
                execution.resumeId(), execution.userId(), execution.messageId(),
                execution.attemptCount(), TraceUtil.getTraceId(), now);
        if (updated != 1) {
            return false;
        }

        ResumeAnalyses analysis = ResumeAnalyses.builder()
                .id(execution.messageId())
                .resumeId(execution.resumeId())
                .overallScore(overallScore)
                .strengthsJson(strengthsJson)
                .suggestionsJson(suggestionsJson)
                .llmConfigSnapshot(snapshotJson)
                .analyzedAt(now)
                .traceId(TraceUtil.getTraceId())
                .createdAt(now)
                .build();
        resumeAnalysesMapper.insert(analysis);
        return true;
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
            ResumeAnalysisExecutionBO execution, String error) {
        OffsetDateTime now = OffsetDateTime.now();
        if (execution.attemptCount() >= MAX_AI_ATTEMPTS) {
            int updated = resumesMapper.failAnalysis(
                    execution.resumeId(), execution.userId(),
                    execution.messageId(), execution.attemptCount(),
                    TraceUtil.getTraceId(), now);
            return updated == 1
                    ? MessageHandleResult.permanentFailure(error)
                    : MessageHandleResult.ignored("resume analysis lease lost");
        }

        int updated = resumesMapper.releaseAnalysisForRetry(
                execution.resumeId(), execution.userId(),
                execution.messageId(), execution.attemptCount(),
                TraceUtil.getTraceId(), now);
        return updated == 1
                ? MessageHandleResult.retry(
                        now.plusMinutes(RETRY_DELAY_MINUTES), error)
                : MessageHandleResult.ignored("resume analysis lease lost");
    }

    /**
     * 将直接异步执行结果同步到待执行兜底消息。
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

    private Resumes findBoundResume(ResumeAnalysisCommand command) {
        return resumesMapper.selectOne(Wrappers.<Resumes>lambdaQuery()
                .select(Resumes::getAnalysisMessageId,
                        Resumes::getAnalysisAttemptCount,
                        Resumes::getAnalysisDeadlineAt,
                        Resumes::getAnalyzeStatus)
                .eq(Resumes::getId, command.resumeId())
                .eq(Resumes::getUserId, command.userId()));
    }
}
