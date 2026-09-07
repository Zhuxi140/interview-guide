package interview.notification.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import interview.api.system.dto.SendNotificationCommand;
import interview.common.enums.ChannelType;
import interview.common.enums.NotifyScene;
import interview.common.enums.NotifyType;
import interview.common.enums.SendStatus;
import interview.notification.mapper.SysNotificationMapper;
import interview.notification.model.entity.SysNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 通知短事务写入服务：只负责 sys_notifications 的落库，不编排渠道。
 *
 * <p>具体单实现类，不建立接口对。幂等依赖 idempotency_key 上的部分唯一索引
 * {@code uk_notifications_idem}（仅约束非空且未删除的行）：先查后插，
 * 并发冲突时捕获唯一约束异常后回查复用既有记录。</p>
 *
 * @author zhuxi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationTxService {

    private final SysNotificationMapper sysNotificationMapper;

    /**
     * 短事务写入一条站内信；加入调用方当前事务。
     *
     * @param command 发送命令（幂等键、场景、接收人、租户）
     * @param title   渲染后的消息标题
     * @param content 渲染后的消息正文
     * @return 通知 ID；幂等命中时返回既有记录 ID
     */
    @Transactional
    public Long insertInApp(SendNotificationCommand command, String title, String content) {
        // 幂等前置查询：同键已存在则直接复用，业务重试不产生重复站内信。
        String idempotencyKey = command.idempotencyKey();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            SysNotification existing = selectByIdempotencyKey(idempotencyKey);
            if (existing != null) {
                return existing.getId();
            }
        }

        // 组装站内信实体：渠道与状态显式写死，notify_type 由场景推导。
        SysNotification entity = SysNotification.builder()
                .enterpriseId(command.enterpriseId())
                .userId(command.userId())
                .notifyType(resolveNotifyType(command.scene()))
                .notifyScene(command.scene().name())
                .channelType(ChannelType.IN_APP.name())
                .sendStatus(SendStatus.SENT.name())
                .idempotencyKey(idempotencyKey)
                .title(title)
                .content(content)
                .isRead(false)
                .build();
        try {
            sysNotificationMapper.insert(entity);
            return entity.getId();
        } catch (DuplicateKeyException exception) {
            // 并发兜底：同键并发插入触发唯一索引，回查复用既有记录。
            log.info("通知幂等键并发冲突，复用既有记录 key={}", idempotencyKey);
            SysNotification existing = selectByIdempotencyKey(idempotencyKey);
            if (existing != null) {
                return existing.getId();
            }
            throw exception;
        }
    }

    /**
     * 按幂等键查询既有通知；唯一索引保证至多一条未删除记录。
     */
    private SysNotification selectByIdempotencyKey(String idempotencyKey) {
        return sysNotificationMapper.selectOne(Wrappers.<SysNotification>lambdaQuery()
                .select(SysNotification::getId)
                .eq(SysNotification::getIdempotencyKey, idempotencyKey)
                .last("LIMIT 1"));
    }

    /**
     * 由业务场景推导站内信类型：面试链路归 INTERVIEW，其余归 SYSTEM。
     */
    private String resolveNotifyType(NotifyScene scene) {
        return switch (scene) {
            case INTERVIEW_INVITE, INTERVIEW_CANCEL, REPORT_READY -> NotifyType.INTERVIEW.name();
            case OFFER_SENT, OFFER_DECIDED, SYSTEM -> NotifyType.SYSTEM.name();
        };
    }
}
