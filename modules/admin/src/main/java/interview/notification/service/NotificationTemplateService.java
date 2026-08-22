package interview.notification.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.notification.model.req.NotificationTemplateCreateReq;
import interview.notification.model.req.NotificationTemplateUpdateReq;
import interview.notification.model.vo.NotificationTemplateCreateVO;
import interview.notification.model.vo.NotificationTemplateListItemVO;
import interview.notification.model.vo.NotificationTemplateUpdateVO;

/**
 * 通知模板管理服务。
 *
 * @author zhuxi
 */
public interface NotificationTemplateService {

    /**
     * 分页查询通知模板。
     *
     * @param page        页码
     * @param size        每页条数
     * @param notifyScene 业务场景筛选，可为空
     * @param channelType 渠道类型筛选，可为空
     * @return 模板分页结果
     */
    IPage<NotificationTemplateListItemVO> pageTemplates(Integer page, Integer size,
                                                         String notifyScene, String channelType);

    /**
     * 创建通知模板。
     *
     * @param req 创建请求
     * @return 创建结果
     */
    NotificationTemplateCreateVO createTemplate(NotificationTemplateCreateReq req);

    /**
     * 编辑通知模板（携带 expectedVersion CAS）。
     *
     * @param templateId 模板 ID
     * @param req        编辑请求
     * @return 编辑结果
     */
    NotificationTemplateUpdateVO updateTemplate(Long templateId, NotificationTemplateUpdateReq req);
}
