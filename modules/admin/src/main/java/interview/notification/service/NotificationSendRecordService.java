package interview.notification.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.notification.model.vo.NotificationSendRecordListItemVO;

/**
 * 全站通知发送记录查询服务。
 *
 * @author zhuxi
 */
public interface NotificationSendRecordService {

    /**
     * 分页查询全站通知发送记录。
     *
     * @param page        页码
     * @param size        每页条数
     * @param channelType 渠道类型筛选，可为空
     * @param sendStatus  发送状态筛选，可为空
     * @param startTime   发送时间下界（ISO-8601 带时区），可为空
     * @param endTime     发送时间上界（ISO-8601 带时区），可为空
     * @return 发送记录分页结果
     */
    IPage<NotificationSendRecordListItemVO> pageRecords(Integer page, Integer size,
                                                        String channelType, String sendStatus,
                                                        String startTime, String endTime);
}
