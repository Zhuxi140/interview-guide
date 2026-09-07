package interview.notification.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.common.enums.ChannelType;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.notification.mapper.SysNotificationMapper;
import interview.notification.model.entity.SysNotification;
import interview.common.enums.NotifyType;
import interview.notification.model.req.NotificationBatchReadReq;
import interview.notification.model.vo.NotificationBatchReadVO;
import interview.notification.model.vo.NotificationListItemVO;
import interview.notification.model.vo.NotificationReadVO;
import interview.notification.model.vo.NotificationUnreadCountVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 收件侧我的消息服务实现。
 *
 * @author zhuxi
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl
        extends ServiceImpl<SysNotificationMapper, SysNotification>
        implements NotificationService {

    @Override
    public IPage<NotificationListItemVO> pageMyNotifications(Integer page, Integer size,
                                                              String notifyType, Boolean isRead) {
        // 校验分页参数与通知类型取值。
        validatePage(page, size);
        String type = normalizeNotifyType(notifyType);

        // 单表 LambdaQuery：仅查询本人站内信，按发送时间倒序稳定分页。
        // channel_type 限定 IN_APP：EMAIL_LOG / SMS_LOG 投递流水不进入用户收件箱。
        IPage<SysNotification> notificationPage = lambdaQuery()
                .select(SysNotification::getId, SysNotification::getNotifyType,
                        SysNotification::getTitle, SysNotification::getContent,
                        SysNotification::getIsRead, SysNotification::getCreatedAt)
                .eq(SysNotification::getUserId, AuthContext.getRequiredUserId())
                .eq(SysNotification::getChannelType, ChannelType.IN_APP.name())
                .eq(type != null, SysNotification::getNotifyType, type)
                .eq(isRead != null, SysNotification::getIsRead, isRead)
                .orderByDesc(SysNotification::getCreatedAt)
                .orderByDesc(SysNotification::getId)
                .page(new Page<>(page, size));

        // 组装列表 VO。
        List<NotificationListItemVO> records = notificationPage.getRecords().stream()
                .map(notification -> new NotificationListItemVO(
                        notification.getId(),
                        notification.getNotifyType(),
                        notification.getTitle(),
                        notification.getContent(),
                        notification.getIsRead(),
                        notification.getCreatedAt()))
                .toList();
        Page<NotificationListItemVO> voPage = new Page<>(
                notificationPage.getCurrent(), notificationPage.getSize(), notificationPage.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public NotificationUnreadCountVO getUnreadCount() {
        // 单表计数：本人未读且未删除（逻辑删除条件由 MyBatis-Plus 自动追加）。
        // channel_type 限定 IN_APP：外部渠道投递流水不计入未读数。
        long unreadCount = lambdaQuery()
                .eq(SysNotification::getUserId, AuthContext.getRequiredUserId())
                .eq(SysNotification::getChannelType, ChannelType.IN_APP.name())
                .eq(SysNotification::getIsRead, false)
                .count();
        return new NotificationUnreadCountVO(unreadCount);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public NotificationReadVO markRead(Long notificationId, Boolean markRead) {
        // 查询消息并校验归属当前用户，避免跨用户标记。
        // channel_type 限定 IN_APP：投递流水不允许被用户侧已读操作触及。
        SysNotification notification = lambdaQuery()
                .select(SysNotification::getId, SysNotification::getUserId,
                        SysNotification::getIsRead, SysNotification::getUpdatedAt)
                .eq(SysNotification::getId, notificationId)
                .eq(SysNotification::getChannelType, ChannelType.IN_APP.name())
                .one();
        if (notification == null
                || !AuthContext.getRequiredUserId().equals(notification.getUserId())) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        // 已读标记幂等：重复提交直接返回当前状态。
        if (Boolean.TRUE.equals(notification.getIsRead()) && Boolean.TRUE.equals(markRead)) {
            return new NotificationReadVO(
                    notification.getId(), true, notification.getUpdatedAt());
        }

        // 非实体更新使用 LambdaUpdate：自动填充不生效，显式写 updated_at。
        OffsetDateTime now = OffsetDateTime.now();
        int updated = baseMapper.update(null, Wrappers.<SysNotification>lambdaUpdate()
                .eq(SysNotification::getId, notificationId)
                .eq(SysNotification::getUserId, AuthContext.getRequiredUserId())
                .eq(SysNotification::getChannelType, ChannelType.IN_APP.name())
                .set(SysNotification::getIsRead, Boolean.TRUE.equals(markRead))
                .set(SysNotification::getUpdatedAt, now));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        // 表内未单列 read_at，以更新时间作为已读时间返回。
        return new NotificationReadVO(notificationId, Boolean.TRUE.equals(markRead), now);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public NotificationBatchReadVO markAllRead(NotificationBatchReadReq req) {
        // 校验 notifyType 取值并限定本人消息范围。
        String type = normalizeNotifyType(req == null ? null : req.getNotifyType());

        // 非实体批量更新使用 LambdaUpdate：自动填充不生效，显式写 updated_at。
        // channel_type 限定 IN_APP：批量已读不得波及 EMAIL_LOG / SMS_LOG 投递流水。
        int updated = baseMapper.update(null, Wrappers.<SysNotification>lambdaUpdate()
                .eq(SysNotification::getUserId, AuthContext.getRequiredUserId())
                .eq(SysNotification::getChannelType, ChannelType.IN_APP.name())
                .eq(SysNotification::getIsRead, false)
                .eq(type != null, SysNotification::getNotifyType, type)
                .set(SysNotification::getIsRead, true)
                .set(SysNotification::getUpdatedAt, OffsetDateTime.now()));
        return new NotificationBatchReadVO(updated);
    }

    private void validatePage(Integer page, Integer size) {
        if (page == null || page < 1 || size == null || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.PAGE_PARAM_INVALID);
        }
    }

    /**
     * 校验并返回合法的通知类型；空白入参返回 null 表示不过滤。
     */
    private String normalizeNotifyType(String notifyType) {
        if (notifyType == null || notifyType.isBlank()) {
            return null;
        }
        try {
            return NotifyType.valueOf(notifyType).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "不支持的通知类型");
        }
    }
}
