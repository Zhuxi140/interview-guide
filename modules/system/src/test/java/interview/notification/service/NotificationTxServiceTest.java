package interview.notification.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import interview.api.system.dto.SendNotificationCommand;
import interview.common.enums.ChannelType;
import interview.common.enums.NotifyScene;
import interview.common.enums.NotifyType;
import interview.notification.mapper.SysNotificationMapper;
import interview.notification.model.entity.SysNotification;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 通知短事务写入测试：幂等命中、正常落库与并发冲突回查。
 */
@ExtendWith(MockitoExtension.class)
class NotificationTxServiceTest {

    @Mock
    private SysNotificationMapper sysNotificationMapper;

    @InjectMocks
    private NotificationTxService service;

    @BeforeEach
    void setUp() {
        // 注册实体元数据，使 lambda 条件构造器能解析列名（Spring 启动时自动完成，单测需手动）。
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "sys_notifications"),
                SysNotification.class);
    }

    private SendNotificationCommand command(String idempotencyKey) {
        return new SendNotificationCommand(
                100L, 200L, NotifyScene.INTERVIEW_INVITE,
                java.util.Set.of(ChannelType.IN_APP),
                java.util.Map.of("interviewTime", "x", "durationMinutes", "60"),
                idempotencyKey, 1L);
    }

    @Test
    void insertInApp_shouldReuseExisting_whenIdempotencyKeyHit() {
        SysNotification existing = SysNotification.builder().id(777L).build();
        when(sysNotificationMapper.selectOne(any())).thenReturn(existing);

        Long id = service.insertInApp(command("INTERVIEW_INVITE:9"), "t", "c");

        assertEquals(777L, id);
        verify(sysNotificationMapper, times(0)).insert(any(SysNotification.class));
    }

    @Test
    void insertInApp_shouldInsertInAppSentEntity() {
        when(sysNotificationMapper.selectOne(any())).thenReturn(null);
        when(sysNotificationMapper.insert(any(SysNotification.class)))
                .thenAnswer(invocation -> {
                    SysNotification entity = invocation.getArgument(0);
                    entity.setId(888L);
                    return 1;
                });

        Long id = service.insertInApp(command("INTERVIEW_INVITE:10"), "面试邀请", "正文");

        assertEquals(888L, id);
        ArgumentCaptor<SysNotification> captor = ArgumentCaptor.forClass(SysNotification.class);
        verify(sysNotificationMapper).insert(captor.capture());
        SysNotification saved = captor.getValue();
        assertEquals(ChannelType.IN_APP.name(), saved.getChannelType());
        assertEquals(interview.common.enums.SendStatus.SENT.name(), saved.getSendStatus());
        assertEquals(NotifyType.INTERVIEW.name(), saved.getNotifyType());
        assertEquals("INTERVIEW_INVITE:10", saved.getIdempotencyKey());
        assertEquals(200L, saved.getUserId());
        assertEquals(false, saved.getIsRead());
    }

    @Test
    void insertInApp_shouldFallbackToLookup_onDuplicateKey() {
        // 并发同键插入触发唯一索引：捕获后回查复用既有记录而非报错。
        SysNotification existing = SysNotification.builder().id(999L).build();
        when(sysNotificationMapper.selectOne(any())).thenReturn(null, existing);
        when(sysNotificationMapper.insert(any(SysNotification.class)))
                .thenThrow(new DuplicateKeyException("uk_notifications_idem"));

        Long id = service.insertInApp(command("INTERVIEW_INVITE:11"), "t", "c");

        assertEquals(999L, id);
        verify(sysNotificationMapper, times(2)).selectOne(any());
    }
}
