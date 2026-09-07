package interview.notification.sender;

import interview.api.system.dto.SendNotificationCommand;
import interview.common.enums.ChannelType;
import interview.notification.content.RenderedContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 短信渠道投递实现：现阶段日志占位，不产生真实外发与流水落库。
 *
 * <p>按项目占位策略，外部服务待整体功能完成后再接入真实供应商；
 * 届时仅替换本实现内部（真实网关调用 + 失败进本地消息表重试），编排层与调用方零改动。
 * 注意本渠道与验证码短信（system/auth 的 SmsServiceImpl）互不复用：后者是
 * "生成-存储-校验"专用链路，不具备业务内容投递语义。</p>
 *
 * @author zhuxi
 */
@Slf4j
@Service
public class SmsNotificationSender implements NotificationSender {

    @Override
    public ChannelType channel() {
        return ChannelType.SMS;
    }

    @Override
    public Long send(SendNotificationCommand command, RenderedContent content) {
        // 占位实现：打印渲染结果供联调验证；生产接入后禁止记录验证码类敏感内容。
        log.info("[占位短信] scene={} userId={} title={} content={}",
                command.scene(), command.userId(), content.title(), content.content());
        return null;
    }
}
