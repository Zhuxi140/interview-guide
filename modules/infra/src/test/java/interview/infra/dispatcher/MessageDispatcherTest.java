package interview.infra.dispatcher;

import interview.common.enums.MsgPriority;
import interview.common.enums.MsgStatus;
import interview.common.enums.MsgTopic;
import interview.common.spi.MessageTransport;
import interview.infra.localMessage.model.entity.LocalMessage;
import interview.infra.localMessage.service.LocalMessageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.List;

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

        new MessageDispatcher(transport, service).dispatch(MsgPriority.LOW);

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

        new MessageDispatcher(transport, service).dispatch(MsgPriority.LOW);

        verify(service).markRetryIfOwned(message, 6, MsgStatus.FAILED, null, "temporary");
        assertEquals(5, message.getRetryCount());
    }
}
