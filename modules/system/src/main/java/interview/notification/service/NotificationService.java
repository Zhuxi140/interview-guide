package interview.notification.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.notification.model.req.NotificationBatchReadReq;
import interview.notification.model.vo.NotificationBatchReadVO;
import interview.notification.model.vo.NotificationListItemVO;
import interview.notification.model.vo.NotificationReadVO;
import interview.notification.model.vo.NotificationUnreadCountVO;

/**
 * 收件侧我的消息服务。
 *
 * @author zhuxi
 */
public interface NotificationService {

    /**
     * 分页查询当前用户的消息列表。
     *
     * @param page       页码
     * @param size       每页条数
     * @param notifyType 通知类型筛选，可为空
     * @param isRead     已读状态筛选，可为空
     * @return 消息分页结果
     */
    IPage<NotificationListItemVO> pageMyNotifications(Integer page, Integer size,
                                                       String notifyType, Boolean isRead);

    /**
     * 查询当前用户的未读消息数量。
     *
     * @return 未读数量
     */
    NotificationUnreadCountVO getUnreadCount();

    /**
     * 标记单条消息为已读（校验归属当前用户）。
     *
     * @param notificationId 消息 ID
     * @param markRead       是否标记已读，仅支持 true
     * @return 标记结果
     */
    NotificationReadVO markRead(Long notificationId, Boolean markRead);

    /**
     * 按筛选范围批量标记当前用户消息为已读。
     *
     * @param req 批量标记请求（可选按 notifyType 收窄范围）
     * @return 更新数量
     */
    NotificationBatchReadVO markAllRead(NotificationBatchReadReq req);
}
