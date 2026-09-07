package interview.notification.sender;

import interview.api.system.dto.SendNotificationCommand;
import interview.common.enums.ChannelType;
import interview.notification.content.RenderedContent;
import interview.notification.service.NotificationTxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 站内信渠道投递实现：真实落库 sys_notifications。
 *
 * @author zhuxi
 */
@Service
@RequiredArgsConstructor
public class InAppNotificationSender implements NotificationSender {

    private final NotificationTxService notificationTxService;

    @Override
    public ChannelType channel() {
        return ChannelType.IN_APP;
    }

    @Override
    public Long send(SendNotificationCommand command, RenderedContent content) {
        // 站内信即本地落库：幂等与审计字段由 TxService 统一处理。
        return notificationTxService.insertInApp(command, content.title(), content.content());
    }
}
