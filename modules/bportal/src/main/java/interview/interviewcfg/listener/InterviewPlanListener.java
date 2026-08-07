package interview.interviewcfg.Listener;

import interview.common.enums.MessageHandleStatus;
import interview.common.spi.MessageHandleResult;
import interview.interviewcfg.event.AgentPlanDraftEvent;
import interview.interviewcfg.service.InterviewPlanGenerationExecutionService;
import interview.interviewcfg.service.InterviewPlanGenerationStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Agent 面试编排草案异步任务监听器。
 *
 * @author zhuxi
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InterviewPlanListener {

    private final InterviewPlanGenerationExecutionService executionService;
    private final InterviewPlanGenerationStateService stateService;

    /**
     * 事务提交后立即执行 Agent 编排，失败时保留消息兜底。
     *
     * @param event 草案生成任务事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("resumeAnalysisExecutor")
    public void handlePlanGeneration(AgentPlanDraftEvent event) {
        try {
            MessageHandleResult result =
                    executionService.execute(event.messageId(), event.draftId());
            stateService.applyDirectResult(event.messageId(), result);
            if (result.status() != MessageHandleStatus.SUCCESS
                    && result.status() != MessageHandleStatus.IGNORED) {
                log.warn("面试编排草案直接执行未完成，等待消息兜底。messageId={}, error={}",
                        event.messageId(), result.error());
            }
        } catch (Exception e) {
            // 未能更新消息时保留 PENDING/PROCESSING，租约到期后仍可被调度器接管。
            log.error("面试编排草案直接执行异常，等待消息兜底。messageId={}",
                    event.messageId(), e);
        }
    }
}