package interview.infra.dispatcher;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 本地消息调度与执行线程池配置。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.message.dispatch")
public class MessageDispatchProperties {

    private long highDelayMs = 5_000;
    private long mediumDelayMs = 30_000;
    private long lowDelayMs = 300_000;
    private long leaseSeconds = 120;
    private int batchSize = 100;
    private int workerThreads = 8;
    private int queueCapacity = 0;
    private int shutdownAwaitSeconds = 30;

    /**
     * 计算执行器能够同时接收的任务总数。
     *
     * @return 执行槽位总数
     */
    public int executionCapacity() {
        return workerThreads + queueCapacity;
    }
}
