package interview.textinterview.listener;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import interview.textinterview.event.InterviewSessionReadyEvent;
import interview.textinterview.service.InterviewQuestionExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 面试会话事件监听器：处理就绪事件并异步触发首题生成与落库
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InterviewSessionEventListener {

    private final InterviewQuestionExecutionService questionExecutionService;

    /**
     * 监听会话就绪事件：事务提交后异步触发 AI 首题生成与数据落库
     *
     * @param event 面试会话就绪事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("interviewQuestionExecutor")
    public void handleSessionReady(InterviewSessionReadyEvent event) {
        try {
            // 监听器仅负责接收事件，具体出题和落库流程交由执行服务完成。
            questionExecutionService.generateFirstQuestion(event);
        } catch (Exception e) {
            log.error("异步生成面试首题发生未捕获异常: sessionId={}, scheduleId={}",
                    event.sessionId(), event.scheduleId(), e);
        }
    }
}
