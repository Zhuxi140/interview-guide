package interview.notification.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import interview.api.system.dto.SendNotificationCommand;
import interview.common.enums.ChannelType;
import interview.common.enums.ErrorCode;
import interview.common.enums.NotifyScene;
import interview.common.exception.BusinessException;
import interview.notification.content.NotificationContentRenderer;
import interview.notification.content.RenderedContent;
import interview.notification.mapper.SysNotificationChannelMapper;
import interview.notification.mapper.SysNotificationMapper;
import interview.notification.model.entity.SysNotification;
import interview.notification.model.entity.SysNotificationChannel;
import interview.notification.sender.NotificationSender;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 通知发送编排测试：渠道过滤、失败隔离与返回值语义。
 */
@ExtendWith(MockitoExtension.class)
class NotificationSendServiceTest {

    @Mock
    private NotificationSender inAppSender;

    @Mock
    private NotificationSender emailSender;

    @Mock
    private SysNotificationChannelMapper channelMapper;

    @Mock
    private SysNotificationMapper notificationMapper;

    @Mock
    private NotificationContentRenderer contentRenderer;

    private NotificationSendService service;

    @BeforeEach
    void setUp() {
        // 注册实体元数据，使渠道启用与重发回写的 lambda 条件构造器能解析列名。
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "sys_notification_channels"),
                SysNotificationChannel.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "sys_notifications"),
                SysNotification.class);
        lenient().when(contentRenderer.renderForChannel(any(), any(), any()))
                .thenReturn(new RenderedContent("标题", "正文"));
        lenient().when(inAppSender.channel()).thenReturn(ChannelType.IN_APP);
        lenient().when(emailSender.channel()).thenReturn(ChannelType.EMAIL);
        service = new NotificationSendService(List.of(inAppSender, emailSender), channelMapper,
                notificationMapper, contentRenderer);
    }

    private SendNotificationCommand command(Set<ChannelType> channels) {
        return new SendNotificationCommand(
                100L, 200L, NotifyScene.OFFER_SENT, channels, null, "OFFER_SENT:1", 1L);
    }

    @Test
    void send_shouldReturnInAppId_andSkipDisabledExternalChannel() {
        // EMAIL 渠道配置为未启用：不应触发其 sender，站内信 ID 正常返回。
        SysNotificationChannel disabled = SysNotificationChannel.builder().isEnabled(false).build();
        when(channelMapper.selectOne(any())).thenReturn(disabled);
        when(inAppSender.send(any(), any())).thenReturn(9001L);

        Long id = service.send(command(Set.of(ChannelType.IN_APP, ChannelType.EMAIL)));

        assertEquals(9001L, id);
        verify(inAppSender).send(any(), any());
        verify(emailSender, never()).send(any(), any());
    }

    @Test
    void send_shouldContinueRemainingChannels_whenOneChannelThrows() {
        // 单渠道异常必须被隔离：EMAIL 抛错不影响站内信落库返回。
        SysNotificationChannel enabled = SysNotificationChannel.builder().isEnabled(true).build();
        when(channelMapper.selectOne(any())).thenReturn(enabled);
        when(inAppSender.send(any(), any())).thenReturn(9002L);
        when(emailSender.send(any(), any())).thenThrow(new IllegalStateException("网关超时"));

        Long id = service.send(command(Set.of(ChannelType.EMAIL, ChannelType.IN_APP)));

        assertEquals(9002L, id);
        verify(emailSender).send(any(), any());
    }

    @Test
    void send_shouldReturnNull_whenOnlyExternalChannelsDelivered() {
        SysNotificationChannel enabled = SysNotificationChannel.builder().isEnabled(true).build();
        when(channelMapper.selectOne(any())).thenReturn(enabled);
        when(emailSender.send(any(), any())).thenReturn(null);

        assertNull(service.send(command(Set.of(ChannelType.EMAIL))));
    }

    @Test
    void send_shouldTreatMissingChannelRowAsDisabled() {
        when(channelMapper.selectOne(any())).thenReturn(null);

        assertNull(service.send(command(Set.of(ChannelType.EMAIL))));
        verify(emailSender, never()).send(any(), any());
    }

    @Test
    void send_shouldReject_whenChannelsEmpty() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.send(command(Set.of())));

        assertEquals(ErrorCode.PARAM_VALID_ERROR.getCode(), exception.getCode());
    }

    @Test
    void send_shouldReject_whenReceiverMissing() {
        SendNotificationCommand invalid = new SendNotificationCommand(
                100L, null, NotifyScene.OFFER_SENT, Set.of(ChannelType.IN_APP), null, null, null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.send(invalid));

        assertEquals(ErrorCode.PARAM_VALID_ERROR.getCode(), exception.getCode());
    }

    @Test
    void resend_shouldRejectInAppRecord() {
        SysNotification record = SysNotification.builder()
                .id(1L).channelType(ChannelType.IN_APP.name())
                .sendStatus(interview.common.enums.SendStatus.SENT.name()).build();
        when(notificationMapper.selectById(1L)).thenReturn(record);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.resend(1L));

        assertEquals(ErrorCode.NOTIFICATION_RESEND_NOT_ALLOWED.getCode(), exception.getCode());
    }

    @Test
    void resend_shouldRejectRecordNotFailed() {
        SysNotification record = SysNotification.builder()
                .id(2L).channelType(ChannelType.EMAIL.name())
                .sendStatus(interview.common.enums.SendStatus.SENT.name()).build();
        when(notificationMapper.selectById(2L)).thenReturn(record);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.resend(2L));

        assertEquals(ErrorCode.NOTIFICATION_RESEND_NOT_ALLOWED.getCode(), exception.getCode());
    }

    @Test
    void resend_shouldRejectWhenConcurrentResendWins() {
        // FAILED→PENDING 条件更新零行：并发重发已被其他执行者接管。
        SysNotification record = SysNotification.builder()
                .id(3L).channelType(ChannelType.EMAIL.name())
                .sendStatus(interview.common.enums.SendStatus.FAILED.name()).build();
        when(notificationMapper.selectById(3L)).thenReturn(record);
        when(notificationMapper.update(any(), any())).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.resend(3L));

        assertEquals(ErrorCode.NOTIFICATION_RESEND_NOT_ALLOWED.getCode(), exception.getCode());
        verify(emailSender, never()).send(any(), any());
    }

    @Test
    void resend_shouldMarkSentAndReturnSent_whenSenderSucceeds() {
        SysNotification record = SysNotification.builder()
                .id(4L).enterpriseId(100L).userId(200L)
                .channelType(ChannelType.EMAIL.name())
                .notifyScene(NotifyScene.OFFER_SENT.name())
                .sendStatus(interview.common.enums.SendStatus.FAILED.name())
                .title("标题").content("正文").build();
        when(notificationMapper.selectById(4L)).thenReturn(record);
        when(notificationMapper.update(any(), any())).thenReturn(1);
        when(emailSender.send(any(), any())).thenReturn(null);

        String status = service.resend(4L);

        assertEquals(interview.common.enums.SendStatus.SENT.name(), status);
        verify(emailSender).send(any(), any());
        // 条件更新 PENDING + 回写 SENT 共两次状态更新。
        verify(notificationMapper, times(2)).update(any(), any());
    }
}
