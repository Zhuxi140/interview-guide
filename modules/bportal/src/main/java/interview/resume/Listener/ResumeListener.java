package interview.resume.Listener;

import interview.common.enums.MessageHandleStatus;
import interview.common.spi.MessageHandleResult;
import interview.resume.event.ResumeAnalysisCreatedEvent;
import interview.resume.event.CandidateProfileCreatedEvent;
import interview.resume.model.message.CandidateProfileCommand;
import interview.resume.model.message.ResumeAnalysisCommand;
import interview.resume.service.CandidateProfileExecutionService;
import interview.resume.service.CandidateProfileStateService;
import interview.resume.service.ResumeAnalysisExecutionService;
import interview.resume.service.ResumeAnalysisStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 *  简历异步任务处理器
 *  @author zhuxi
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class ResumeListener {

    private final ResumeAnalysisExecutionService executionService;
    private final ResumeAnalysisStateService stateService;
    private final CandidateProfileExecutionService profileExecutionService;
    private final CandidateProfileStateService profileStateService;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    @Async("resumeAnalysisExecutor")
    public void handleResumeAnalysis(ResumeAnalysisCreatedEvent event) {
        try {
            ResumeAnalysisCommand command =
                    new ResumeAnalysisCommand(event.resumeId(), event.userId());
            MessageHandleResult result =
                    executionService.execute(event.messageId(), command);
            stateService.applyDirectResult(event.messageId(), result);
            if (result.status() != MessageHandleStatus.SUCCESS
                    && result.status() != MessageHandleStatus.IGNORED) {
                log.warn("简历 AI 分析直接执行未完成，等待消息兜底。messageId={}, error={}",
                        event.messageId(), result.error());
            }
        } catch (Exception e) {
            // 未能更新消息时保留 PENDING/PROCESSING，租约到期后仍可被调度器接管。
            log.error("简历 AI 分析直接执行异常，等待消息兜底。messageId={}",
                    event.messageId(), e);
        }
    }

    /**
     * 事务提交后立即执行人才画像，失败时保留消息兜底。
     *
     * @param event 人才画像任务事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("resumeAnalysisExecutor")
    public void handleCandidateProfile(CandidateProfileCreatedEvent event) {
        try {
            CandidateProfileCommand command = new CandidateProfileCommand(
                    event.resumeId(), event.candidateId());
            MessageHandleResult result = profileExecutionService.execute(
                    event.messageId(), command);
            profileStateService.applyDirectResult(event.messageId(), result);
            if (result.status() != MessageHandleStatus.SUCCESS
                    && result.status() != MessageHandleStatus.IGNORED) {
                log.warn("人才画像直接执行未完成，等待消息兜底。messageId={}, error={}",
                        event.messageId(), result.error());
            }
        } catch (Exception e) {
            log.error("人才画像直接执行异常，等待消息兜底。messageId={}",
                    event.messageId(), e);
        }
    }

}
