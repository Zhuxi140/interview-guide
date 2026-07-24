package interview.infra.config;

import interview.infra.dispatcher.MessageDispatchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 本地消息业务处理线程池配置。
 */
@Configuration
@EnableConfigurationProperties(MessageDispatchProperties.class)
public class MessageExecutorConfig {

    /**
     * 创建有界消息执行器，避免调度线程直接执行业务处理器。
     *
     * @param properties 消息调度配置
     * @return 消息执行器
     */
    @Bean("messageHandlerExecutor")
    public ThreadPoolTaskExecutor messageHandlerExecutor(MessageDispatchProperties properties) {
        validate(properties);

        // 固定工作线程数量，默认不缓存已领取但尚未开始执行的租约消息。
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getWorkerThreads());
        executor.setMaxPoolSize(properties.getWorkerThreads());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setThreadNamePrefix("local-message-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(properties.getShutdownAwaitSeconds());
        executor.initialize();
        return executor;
    }

    private void validate(MessageDispatchProperties properties) {
        if (properties.getWorkerThreads() <= 0) {
            throw new IllegalArgumentException("app.message.dispatch.worker-threads must be positive");
        }
        if (properties.getQueueCapacity() < 0) {
            throw new IllegalArgumentException("app.message.dispatch.queue-capacity cannot be negative");
        }
        if (properties.getBatchSize() <= 0) {
            throw new IllegalArgumentException("app.message.dispatch.batch-size must be positive");
        }
        if (properties.getLeaseSeconds() <= 0) {
            throw new IllegalArgumentException("app.message.dispatch.lease-seconds must be positive");
        }
        if (properties.getShutdownAwaitSeconds() < 0) {
            throw new IllegalArgumentException(
                    "app.message.dispatch.shutdown-await-seconds cannot be negative"
            );
        }
    }
}
