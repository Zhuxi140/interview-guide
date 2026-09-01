package interview.textinterview.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 面试 AI 异步任务专属隔离线程池配置
 */
@Configuration
@EnableAsync
public class InterviewAsyncConfig {

    /**
     * 面试 AI 出题与题目生成专属异步执行线程池
     *
     * @return 线程池执行器
     */
    @Bean("interviewQuestionExecutor")
    public Executor interviewQuestionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("interview-ai-q-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
