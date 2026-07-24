package interview.resume.service.impl;

import lombok.RequiredArgsConstructor;
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
public class ResumeAnalysisTaskHandler {


    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    @Async("resumeAnalysisExecutor")
    public void handleResumeAnalysis(String resumeText) {

        // Implementation for handling resume analysis task
    }

}
