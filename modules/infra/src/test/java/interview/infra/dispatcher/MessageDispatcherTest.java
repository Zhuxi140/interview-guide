package interview.infra.dispatcher;

import interview.common.enums.MsgPriority;
import interview.common.enums.MsgStatus;
import interview.common.enums.MsgTopic;
import interview.common.spi.MessageHandleResult;
import interview.common.spi.MessageTransport;
import interview.infra.config.MessageExecutorConfig;
import interview.infra.localMessage.model.entity.LocalMessage;
import interview.infra.localMessage.service.LocalMessageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MessageDispatcherTest {

    @Test
    void dispatch_shouldScheduleFifthRetryAtSixtyMinutes() {
        // 第五次重试仍应被调度，失败状态从下一次失败才开始。
        MessageTransport transport = mock(MessageTransport.class);
        LocalMessageService service = mock(LocalMessageService.class);
        LocalMessage message = LocalMessage.builder()
                .id(1L)
                .topic(MsgTopic.RESUME_UPLOAD_CLEANUP)
                .priority(MsgPriority.LOW)
                .status(MsgStatus.PROCESSING)
                .retryCount(4)
                .maxRetries(5)
                .leaseOwner("worker-1")
                .leaseVersion(1L)
                .build();
        when(service.claimForDispatch(eq(MsgPriority.LOW), anyString(), eq(120L), eq(100)))
                .thenReturn(List.of(message));
        when(transport.dispatch(any())).thenThrow(new IllegalStateException("temporary"));
        when(service.markRetryIfOwned(eq(message), eq(5), eq(MsgStatus.PENDING), any(), eq("temporary")))
                .thenReturn(true);
        OffsetDateTime before = OffsetDateTime.now().plusMinutes(59);

        dispatcher(transport, service, Runnable::run, 100).dispatch(MsgPriority.LOW);

        ArgumentCaptor<OffsetDateTime> retryAt = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(service).markRetryIfOwned(eq(message), eq(5), eq(MsgStatus.PENDING), retryAt.capture(),
                eq("temporary"));
        assertTrue(retryAt.getValue().isAfter(before));
    }

    @Test
    void dispatch_shouldFailAfterAllFiveRetriesAreConsumed() {
        // 第六次失败达到终态，不再生成下一次执行时间。
        MessageTransport transport = mock(MessageTransport.class);
        LocalMessageService service = mock(LocalMessageService.class);
        LocalMessage message = LocalMessage.builder()
                .id(1L)
                .topic(MsgTopic.RESUME_UPLOAD_CLEANUP)
                .priority(MsgPriority.LOW)
                .status(MsgStatus.PROCESSING)
                .retryCount(5)
                .maxRetries(5)
                .leaseOwner("worker-1")
                .leaseVersion(1L)
                .build();
        when(service.claimForDispatch(eq(MsgPriority.LOW), anyString(), eq(120L), eq(100)))
                .thenReturn(List.of(message));
        when(transport.dispatch(any())).thenThrow(new IllegalStateException("temporary"));
        when(service.markRetryIfOwned(message, 6, MsgStatus.FAILED, null, "temporary"))
                .thenReturn(true);

        dispatcher(transport, service, Runnable::run, 100).dispatch(MsgPriority.LOW);

        verify(service).markRetryIfOwned(message, 6, MsgStatus.FAILED, null, "temporary");
        assertEquals(5, message.getRetryCount());
    }

    @Test
    void dispatch_shouldExecuteMessagesInParallelAndStopClaimingWhenPoolIsFull() throws Exception {
        // 两个执行槽位都被占用时，调度线程应立即返回且不得继续领取高优先级消息。
        MessageTransport transport = mock(MessageTransport.class);
        LocalMessageService service = mock(LocalMessageService.class);
        LocalMessage first = leasedMessage(1L, 0);
        LocalMessage second = leasedMessage(2L, 0);
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);

        when(service.claimForDispatch(eq(MsgPriority.MEDIUM), anyString(), eq(120L), eq(2)))
                .thenReturn(List.of(first, second));
        when(transport.dispatch(any())).thenAnswer(invocation -> {
            started.countDown();
            release.await(3, TimeUnit.SECONDS);
            return MessageHandleResult.success();
        });
        when(service.markSuccessIfOwned(any(LocalMessage.class))).thenReturn(true);

        MessageDispatchProperties properties = properties(2);
        ThreadPoolTaskExecutor executor =
                new MessageExecutorConfig().messageHandlerExecutor(properties);
        try {
            MessageDispatcher dispatcher = new MessageDispatcher(transport, service, executor, properties);

            long startedAt = System.nanoTime();
            dispatcher.dispatch(MsgPriority.MEDIUM);
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

            assertTrue(elapsedMillis < 1_000, "调度线程不应等待 Handler 执行结束");
            assertTrue(started.await(2, TimeUnit.SECONDS), "两条消息应由不同工作线程并行执行");

            dispatcher.dispatch(MsgPriority.HIGH);
            verify(service, never()).claimForDispatch(
                    eq(MsgPriority.HIGH), anyString(), anyLong(), anyInt()
            );

            release.countDown();
            verify(service, timeout(2_000).times(2)).markSuccessIfOwned(any(LocalMessage.class));
        } finally {
            release.countDown();
            executor.shutdown();
        }
    }

    @Test
    void dispatch_shouldReleaseLeaseWithoutConsumingRetryWhenExecutorRejectsTask() {
        // 执行器关闭等基础设施故障不属于业务处理失败，不应增加 retryCount。
        MessageTransport transport = mock(MessageTransport.class);
        LocalMessageService service = mock(LocalMessageService.class);
        LocalMessage message = leasedMessage(1L, 2);
        Executor rejectingExecutor = task -> {
            throw new RejectedExecutionException("shutdown");
        };
        when(service.claimForDispatch(eq(MsgPriority.LOW), anyString(), eq(120L), eq(1)))
                .thenReturn(List.of(message));
        when(service.markRetryIfOwned(
                eq(message), eq(2), eq(MsgStatus.PENDING), any(), contains("shutdown")
        )).thenReturn(true);

        dispatcher(transport, service, rejectingExecutor, 1).dispatch(MsgPriority.LOW);

        verifyNoInteractions(transport);
        verify(service).markRetryIfOwned(
                eq(message), eq(2), eq(MsgStatus.PENDING), any(), contains("shutdown")
        );
    }

    private MessageDispatcher dispatcher(MessageTransport transport,
                                         LocalMessageService service,
                                         Executor executor,
                                         int workerThreads) {
        MessageDispatchProperties properties = properties(workerThreads);
        return new MessageDispatcher(transport, service, executor, properties);
    }

    private MessageDispatchProperties properties(int workerThreads) {
        MessageDispatchProperties properties = new MessageDispatchProperties();
        properties.setWorkerThreads(workerThreads);
        properties.setQueueCapacity(0);
        properties.setBatchSize(100);
        properties.setLeaseSeconds(120);
        return properties;
    }

    private LocalMessage leasedMessage(Long id, int retryCount) {
        return LocalMessage.builder()
                .id(id)
                .topic(MsgTopic.RESUME_UPLOAD_CLEANUP)
                .priority(MsgPriority.MEDIUM)
                .status(MsgStatus.PROCESSING)
                .retryCount(retryCount)
                .maxRetries(5)
                .leaseOwner("worker-1")
                .leaseVersion(1L)
                .build();
    }
}
