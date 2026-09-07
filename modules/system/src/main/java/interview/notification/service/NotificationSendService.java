package interview.notification.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import interview.api.system.NotificationApi;
import interview.api.system.dto.SendNotificationCommand;
import interview.common.enums.ChannelType;
import interview.common.enums.ErrorCode;
import interview.common.enums.NotifyScene;
import interview.common.enums.SendStatus;
import interview.common.exception.BusinessException;
import interview.notification.content.NotificationContentRenderer;
import interview.notification.content.RenderedContent;
import interview.notification.model.entity.SysNotification;
import interview.notification.model.entity.SysNotificationChannel;
import interview.notification.sender.NotificationSender;
import interview.notification.mapper.SysNotificationChannelMapper;
import interview.notification.mapper.SysNotificationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 通知发送编排：渲染文案 → 按渠道启用状态分发 → 单渠道失败不中断。
 *
 * <p>实现跨模块契约 {@link NotificationApi}。方法以默认 REQUIRED 传播加入调用方事务，
 * 保证站内信落库与业务数据同事务；未来接入真实外部网关时，仅本类内部切换为
 * Outbox 异步派发，契约与全部调用方零改动。</p>
 *
 * @author zhuxi
 */
@Slf4j
@Service
public class NotificationSendService implements NotificationApi {

    /**
     * 渠道投递实现注册表：由 Spring 注入全部 {@link NotificationSender} 后按渠道类型建立。
     */
    private final Map<ChannelType, NotificationSender> senderMap;

    private final SysNotificationChannelMapper sysNotificationChannelMapper;

    private final SysNotificationMapper sysNotificationMapper;

    private final NotificationContentRenderer contentRenderer;

    public NotificationSendService(List<NotificationSender> senders,
                                   SysNotificationChannelMapper sysNotificationChannelMapper,
                                   SysNotificationMapper sysNotificationMapper,
                                   NotificationContentRenderer contentRenderer) {
        this.senderMap = senders.stream()
                .collect(Collectors.toUnmodifiableMap(NotificationSender::channel, Function.identity()));
        this.sysNotificationChannelMapper = sysNotificationChannelMapper;
        this.sysNotificationMapper = sysNotificationMapper;
        this.contentRenderer = contentRenderer;
    }

    @Override
    @Transactional
    public Long send(SendNotificationCommand command) {
        // 1. 契约校验：接收人、租户、场景与渠道是落库与分发的最小必填集。
        validate(command);

        // 2. 逐渠道投递；模板按渠道区分，因此渲染在渠道循环内完成；
        //    单渠道失败记录日志后继续，不回抛异常、不阻塞业务主流程。
        Long notificationId = null;
        for (ChannelType channel : command.channels()) {
            if (!isChannelEnabled(channel)) {
                log.info("通知渠道未启用，跳过 scene={} channel={}", command.scene(), channel);
                continue;
            }
            NotificationSender sender = senderMap.get(channel);
            if (sender == null) {
                log.warn("渠道缺少投递实现，跳过 channel={}", channel);
                continue;
            }
            try {
                // 渲染优先取管理端场景+渠道模板，未配置回退内置文案；变量缺失在此显式失败。
                RenderedContent content = contentRenderer.renderForChannel(
                        command.scene(), channel, command.vars());
                Long id = sender.send(command, content);
                if (channel == ChannelType.IN_APP) {
                    notificationId = id;
                }
            } catch (BusinessException exception) {
                // 契约级错误（幂等冲突回查失败、模板变量缺失等）属于编程或数据问题，快速失败。
                throw exception;
            } catch (Exception exception) {
                log.error("通知投递失败 scene={} channel={} userId={}",
                        command.scene(), channel, command.userId(), exception);
            }
        }
        return notificationId;
    }

    /**
     * 校验命令必填项；渠道集合为空视为契约错误而非"不发送"。
     */
    private void validate(SendNotificationCommand command) {
        if (command == null || command.userId() == null || command.enterpriseId() == null
                || command.scene() == null) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "通知命令缺少必填项");
        }
        Set<ChannelType> channels = command.channels();
        if (channels == null || channels.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "通知渠道不能为空");
        }
    }

    /**
     * 渠道启用状态：站内信恒为启用；外部渠道查配置行，缺行视为未启用。
     */
    private boolean isChannelEnabled(ChannelType channel) {
        if (channel == ChannelType.IN_APP) {
            return true;
        }
        SysNotificationChannel row = sysNotificationChannelMapper.selectOne(
                Wrappers.<SysNotificationChannel>lambdaQuery()
                        .select(SysNotificationChannel::getIsEnabled)
                        .eq(SysNotificationChannel::getChannelType, channel.name()));
        return row != null && Boolean.TRUE.equals(row.getIsEnabled());
    }

    /**
     * 重发失败的外部渠道通知（U9-02，管理端人工兜底）。
     *
     * <p>仅 EMAIL / SMS 且 {@code send_status=FAILED} 的记录允许重发；
     * FAILED → PENDING 条件更新保证并发重发只有一个执行者成功。
     * 占位阶段 Sender 同步完成，直接回写最终状态并返回；未来 MQ 版本在
     * PENDING 后异步派发，接口层再改为"受理即返回 PENDING"语义。</p>
     *
     * @param notificationId 发送记录 ID
     * @return 重发后的发送状态（SENT / FAILED）
     */
    @Transactional
    public String resend(Long notificationId) {
        // 1. 查询记录并校验：站内信与未失败记录不允许重发。
        SysNotification record = sysNotificationMapper.selectById(notificationId);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        if (ChannelType.IN_APP.name().equals(record.getChannelType())
                || !SendStatus.FAILED.name().equals(record.getSendStatus())) {
            throw new BusinessException(ErrorCode.NOTIFICATION_RESEND_NOT_ALLOWED);
        }
        ChannelType channel = ChannelType.valueOf(record.getChannelType());

        // 2. 条件更新 FAILED → PENDING：并发重发时零行更新方直接拒绝。
        int updated = sysNotificationMapper.update(null, Wrappers.<SysNotification>lambdaUpdate()
                .eq(SysNotification::getId, notificationId)
                .eq(SysNotification::getSendStatus, SendStatus.FAILED.name())
                .set(SysNotification::getSendStatus, SendStatus.PENDING.name())
                .set(SysNotification::getUpdatedAt, OffsetDateTime.now()));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOTIFICATION_RESEND_NOT_ALLOWED);
        }

        // 3. 复用渠道 Sender 重发既有渲染内容：成功回写 SENT，异常回写 FAILED 与原因。
        try {
            NotificationSender sender = senderMap.get(channel);
            if (sender != null) {
                sender.send(toResendCommand(record, channel),
                        new RenderedContent(record.getTitle(), record.getContent()));
            }
            markResendResult(notificationId, SendStatus.SENT, null);
            return SendStatus.SENT.name();
        } catch (Exception exception) {
            log.error("通知重发失败 id={} channel={}", notificationId, channel, exception);
            markResendResult(notificationId, SendStatus.FAILED, exception.getMessage());
            return SendStatus.FAILED.name();
        }
    }

    /**
     * 由流水记录构造重发命令：身份信息全部取自记录本身，与原始发送解耦。
     */
    private SendNotificationCommand toResendCommand(SysNotification record, ChannelType channel) {
        NotifyScene scene;
        try {
            scene = record.getNotifyScene() == null
                    ? NotifyScene.SYSTEM : NotifyScene.valueOf(record.getNotifyScene());
        } catch (IllegalArgumentException exception) {
            scene = NotifyScene.SYSTEM;
        }
        return new SendNotificationCommand(
                record.getEnterpriseId(), record.getUserId(), scene,
                Set.of(channel), null, null, null);
    }

    /**
     * 回写重发结果；失败时同时记录失败原因供管理端排查。
     */
    private void markResendResult(Long notificationId, SendStatus status, String failureReason) {
        sysNotificationMapper.update(null, Wrappers.<SysNotification>lambdaUpdate()
                .eq(SysNotification::getId, notificationId)
                .set(SysNotification::getSendStatus, status.name())
                .set(failureReason != null, SysNotification::getFailureReason, failureReason)
                .set(SysNotification::getUpdatedAt, OffsetDateTime.now()));
    }
}
