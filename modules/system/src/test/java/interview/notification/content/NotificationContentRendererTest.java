package interview.notification.content;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import interview.common.enums.ChannelType;
import interview.common.enums.NotifyScene;
import interview.notification.mapper.SysNotificationTemplateMapper;
import interview.notification.model.entity.SysNotificationTemplate;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 通知内容渲染器测试：模板命中优先，未配置回退内置文案。
 */
@ExtendWith(MockitoExtension.class)
class NotificationContentRendererTest {

    @Mock
    private SysNotificationTemplateMapper templateMapper;

    private NotificationContentRenderer renderer;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "sys_notification_templates"),
                SysNotificationTemplate.class);
        renderer = new NotificationContentRenderer(templateMapper);
    }

    @Test
    void renderForChannel_shouldUseTemplate_whenConfiguredAndEnabled() {
        SysNotificationTemplate template = SysNotificationTemplate.builder()
                .title("【测试企业】面试通知")
                .contentTemplate("您有一场面试：${interviewTime}，请准时参加。")
                .isEnabled(true)
                .build();
        when(templateMapper.selectOne(any())).thenReturn(template);

        RenderedContent content = renderer.renderForChannel(
                NotifyScene.INTERVIEW_INVITE, ChannelType.IN_APP,
                Map.of("interviewTime", "09-10 14:00", "durationMinutes", "60"));

        assertEquals("【测试企业】面试通知", content.title());
        assertEquals("您有一场面试：09-10 14:00，请准时参加。", content.content());
    }

    @Test
    void renderForChannel_shouldFallbackToBuiltin_whenTemplateMissing() {
        when(templateMapper.selectOne(any())).thenReturn(null);

        RenderedContent content = renderer.renderForChannel(
                NotifyScene.OFFER_SENT, ChannelType.IN_APP, null);

        assertEquals("Offer 已发送", content.title());
    }
}
