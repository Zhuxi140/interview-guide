package interview.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.notification.model.entity.NotificationSendRecord;

/**
 * 全站通知发送记录 Mapper（复用 sys_notifications 表）。
 *
 * @author zhuxi
 */
public interface NotificationSendRecordMapper extends BaseMapper<NotificationSendRecord> {
}
