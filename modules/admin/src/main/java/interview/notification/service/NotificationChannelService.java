package interview.notification.service;

import interview.notification.model.req.NotificationChannelsUpdateReq;
import interview.notification.model.vo.NotificationChannelsVO;

/**
 * 通知发送渠道配置服务。
 *
 * @author zhuxi
 */
public interface NotificationChannelService {

    /**
     * 查询全部渠道配置与启用状态（凭证字段脱敏）。
     *
     * @return 渠道配置集合与聚合版本号
     */
    NotificationChannelsVO getChannels();

    /**
     * 按聚合版本号完整更新渠道配置（CAS）。
     *
     * @param req 全量更新请求
     * @return 更新后的渠道配置集合与聚合版本号
     */
    NotificationChannelsVO updateChannels(NotificationChannelsUpdateReq req);
}
