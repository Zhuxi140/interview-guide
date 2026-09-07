package interview.notification.content;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import interview.common.enums.ChannelType;
import interview.common.enums.NotifyScene;
import interview.notification.mapper.SysNotificationTemplateMapper;
import interview.notification.model.entity.SysNotificationTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 通知内容渲染器：优先取管理端配置的场景+渠道启用模板，未配置时回退内置文案。
 *
 * <p>具体单实现类，不建立接口对。模板命中与否对调用方透明——
 * 渠道未配置模板时系统仍以 {@link NotificationSceneText} 的场景文案工作，
 * 管理端可通过模板覆盖默认文案而不影响发送链路。</p>
 *
 * @author zhuxi
 */
@Service
@RequiredArgsConstructor
public class NotificationContentRenderer {

    private final SysNotificationTemplateMapper sysNotificationTemplateMapper;

    /**
     * 按场景与渠道渲染标题与正文。
     *
     * @param scene   业务场景
     * @param channel 投递渠道（模板按渠道区分，如同一场景的邮件与短信文案不同）
     * @param vars    模板变量；模板与内置文案均无占位符时允许 null
     * @return 渲染结果
     */
    public RenderedContent renderForChannel(NotifyScene scene, ChannelType channel,
                                            java.util.Map<String, String> vars) {
        // 查询该场景+渠道的启用模板；命中即以模板为准（占位符白名单以模板实际内容为准）。
        SysNotificationTemplate template = sysNotificationTemplateMapper.selectOne(
                Wrappers.<SysNotificationTemplate>lambdaQuery()
                        .select(SysNotificationTemplate::getTitle,
                                SysNotificationTemplate::getContentTemplate)
                        .eq(SysNotificationTemplate::getNotifyScene, scene.name())
                        .eq(SysNotificationTemplate::getChannelType, channel.name())
                        .eq(SysNotificationTemplate::getIsEnabled, true)
                        .last("LIMIT 1"));

        // 未配置模板时回退内置场景文案，保证无模板配置也能正常发送。
        if (template == null) {
            return NotificationSceneText.render(scene, vars);
        }
        return NotificationSceneText.renderTemplate(
                template.getTitle(), template.getContentTemplate(), vars);
    }
}
