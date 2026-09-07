package interview.notification.sender;

import interview.api.system.dto.SendNotificationCommand;
import interview.common.enums.ChannelType;
import interview.notification.content.RenderedContent;

/**
 * 通知渠道投递策略。
 *
 * <p>每个渠道一个实现，编排层按渠道类型分发；单实现不建接口对的约定在此例外，
 * 因为渠道天然是多实现策略场景。实现不得向调用方回抛网络类异常，
 * 投递失败由编排层统一捕获记录。</p>
 *
 * @author zhuxi
 */
public interface NotificationSender {

    /**
     * 本实现负责的投递渠道。
     *
     * @return 渠道类型
     */
    ChannelType channel();

    /**
     * 向命令指定的接收人投递已渲染内容。
     *
     * @param command 发送命令（接收人、场景、租户、幂等键）
     * @param content 渲染后的标题与正文
     * @return 站内信渠道返回落库通知 ID；外部渠道无落库返回 null
     */
    Long send(SendNotificationCommand command, RenderedContent content);
}
