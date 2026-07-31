package interview.matching.listener;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import interview.common.enums.AiTaskStatus;
import interview.common.enums.MessageHandleStatus;
import interview.common.spi.MessageHandleResult;
import interview.matching.event.ApplicationScreeningCreatedEvent;
import interview.matching.event.CandidateJobMatchCreatedEvent;
import interview.matching.mapper.ApplicationAiScreeningMapper;
import interview.matching.mapper.CandidateJobMatchAnalysisMapper;
import interview.matching.model.entity.ApplicationAiScreening;
import interview.matching.model.entity.CandidateJobMatchAnalysis;
import interview.matching.model.message.CandidateJobMatchCommand;
import interview.matching.model.message.ApplicationScreeningCommand;
import interview.matching.service.ApplicationScreeningExecutionService;
import interview.matching.service.ApplicationScreeningStateService;
import interview.matching.service.CandidateJobMatchExecutionService;
import interview.matching.service.CandidateJobMatchStateService;
import interview.resume.event.CandidateProfileCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 投递相关 AI 任务的事务提交后直接执行器。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchingAiListener {

    private final ApplicationScreeningExecutionService screeningExecutionService;
    private final ApplicationScreeningStateService screeningStateService;
    private final CandidateJobMatchExecutionService jobMatchExecutionService;
    private final CandidateJobMatchStateService jobMatchStateService;
    private final ApplicationAiScreeningMapper screeningMapper;
    private final CandidateJobMatchAnalysisMapper jobMatchMapper;

    /**
     * 事务提交后立即执行 HR AI 初筛，失败时保留消息兜底。
     *
     * @param event 初筛任务事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("resumeAnalysisExecutor")
    public void handleApplicationScreening(ApplicationScreeningCreatedEvent event) {
        try {
            ApplicationScreeningCommand command =
                    new ApplicationScreeningCommand(event.applicationId());
            MessageHandleResult result = screeningExecutionService.execute(
                    event.messageId(), command);
            screeningStateService.applyDirectResult(event.messageId(), result);
            if (result.status() != MessageHandleStatus.SUCCESS
                    && result.status() != MessageHandleStatus.IGNORED) {
                log.warn("HR AI 初筛直接执行未完成，等待消息兜底。messageId={}, error={}",
                        event.messageId(), result.error());
            }
        } catch (Exception e) {
            log.error("HR AI 初筛直接执行异常，等待消息兜底。messageId={}",
                    event.messageId(), e);
        }
    }

    /**
     * 事务提交后立即执行候选人岗位预测，失败时保留消息兜底。
     *
     * @param event 岗位预测任务事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("resumeAnalysisExecutor")
    public void handleCandidateJobMatch(CandidateJobMatchCreatedEvent event) {
        try {
            CandidateJobMatchCommand command = new CandidateJobMatchCommand(
                    event.applicationId());
            MessageHandleResult result = jobMatchExecutionService.execute(
                    event.messageId(), command);
            jobMatchStateService.applyDirectResult(event.messageId(), result);
            if (result.status() != MessageHandleStatus.SUCCESS
                    && result.status() != MessageHandleStatus.IGNORED) {
                log.warn("候选人岗位预测直接执行未完成，等待消息兜底。messageId={}, error={}",
                        event.messageId(), result.error());
            }
        } catch (Exception e) {
            log.error("候选人岗位预测直接执行异常，等待消息兜底。messageId={}",
                    event.messageId(), e);
        }
    }

    /**
     * 画像完成后立即唤醒等待中的 HR 初筛和候选人预测。
     *
     * @param event 画像完成事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("resumeAnalysisExecutor")
    public void handleCandidateProfileCompleted(CandidateProfileCompletedEvent event) {
        screeningMapper.selectList(Wrappers.<ApplicationAiScreening>lambdaQuery()
                        .select(ApplicationAiScreening::getId,
                                ApplicationAiScreening::getApplicationId)
                        .eq(ApplicationAiScreening::getCandidateProfileId,
                                event.profileId())
                        .eq(ApplicationAiScreening::getStatus,
                                AiTaskStatus.WAITING_PROFILE))
                .forEach(this::executeWaitingScreening);
        jobMatchMapper.selectList(Wrappers.<CandidateJobMatchAnalysis>lambdaQuery()
                        .select(CandidateJobMatchAnalysis::getId,
                                CandidateJobMatchAnalysis::getApplicationId)
                        .eq(CandidateJobMatchAnalysis::getCandidateProfileId,
                                event.profileId())
                        .eq(CandidateJobMatchAnalysis::getStatus,
                                AiTaskStatus.WAITING_PROFILE))
                .forEach(this::executeWaitingJobMatch);
    }

    private void executeWaitingScreening(ApplicationAiScreening screening) {
        try {
            MessageHandleResult result = screeningExecutionService.execute(
                    screening.getId(),
                    new ApplicationScreeningCommand(screening.getApplicationId()));
            screeningStateService.applyDirectResult(screening.getId(), result);
        } catch (Exception e) {
            log.error("画像完成后唤醒 HR 初筛失败，等待消息兜底。messageId={}",
                    screening.getId(), e);
        }
    }

    private void executeWaitingJobMatch(CandidateJobMatchAnalysis analysis) {
        try {
            MessageHandleResult result = jobMatchExecutionService.execute(
                    analysis.getId(),
                    new CandidateJobMatchCommand(analysis.getApplicationId()));
            jobMatchStateService.applyDirectResult(analysis.getId(), result);
        } catch (Exception e) {
            log.error("画像完成后唤醒候选人岗位预测失败，等待消息兜底。messageId={}",
                    analysis.getId(), e);
        }
    }
}
