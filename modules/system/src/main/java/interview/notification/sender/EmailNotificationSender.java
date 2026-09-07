package interview.notification.sender;

import interview.api.system.dto.SendNotificationCommand;
import interview.common.enums.ChannelType;
import interview.notification.content.RenderedContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 邮件渠道投递实现：现阶段日志占位，不产生真实外发与流水落库。
 *
 * <p>按项目占位策略，外部服务待整体功能完成后再接入真实供应商；
 * 届时仅替换本实现内部（真实网关调用 + 失败进本地消息表重试），编排层与调用方零改动。</p>
 *
 * @author zhuxi
 */
@Slf4j
@Service
public class EmailNotificationSender implements NotificationSender {

    @Override
    public ChannelType channel() {
        return ChannelType.EMAIL;
    }

    @Override
    public Long send(SendNotificationCommand command, RenderedContent content) {
        // 占位实现：打印渲染结果供联调验证；userId 仅打印尾号语义，不输出敏感正文之外的地址信息。
        log.info("[占位邮件] scene={} userId={} title={} content={}",
                command.scene(), command.userId(), content.title(), content.content());
        return null;
    }
}
