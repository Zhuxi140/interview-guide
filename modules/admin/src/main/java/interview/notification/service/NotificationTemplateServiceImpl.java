package interview.notification.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.notification.mapper.SysNotificationTemplateMapper;
import interview.notification.model.entity.SysNotificationTemplate;
import interview.notification.model.enums.ChannelType;
import interview.notification.model.enums.NotifyScene;
import interview.notification.model.req.NotificationTemplateCreateReq;
import interview.notification.model.req.NotificationTemplateUpdateReq;
import interview.notification.model.vo.NotificationTemplateCreateVO;
import interview.notification.model.vo.NotificationTemplateListItemVO;
import interview.notification.model.vo.NotificationTemplateUpdateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 通知模板管理服务实现。
 *
 * @author zhuxi
 */
@Service
@RequiredArgsConstructor
public class NotificationTemplateServiceImpl
        extends ServiceImpl<SysNotificationTemplateMapper, SysNotificationTemplate>
        implements NotificationTemplateService {

    @Override
    public IPage<NotificationTemplateListItemVO> pageTemplates(Integer page, Integer size,
                                                                String notifyScene,
                                                                String channelType) {
        // 校验分页参数与枚举筛选取值。
        validatePage(page, size);
        String scene = normalizeScene(notifyScene);
        String channel = normalizeChannel(channelType);

        // 单表 LambdaQuery：按更新时间倒序稳定分页。
        IPage<SysNotificationTemplate> templatePage = lambdaQuery()
                .eq(scene != null, SysNotificationTemplate::getNotifyScene, scene)
                .eq(channel != null, SysNotificationTemplate::getChannelType, channel)
                .orderByDesc(SysNotificationTemplate::getUpdatedAt)
                .orderByDesc(SysNotificationTemplate::getId)
                .page(new Page<>(page, size));

        // 组装列表 VO。
        List<NotificationTemplateListItemVO> records = templatePage.getRecords().stream()
                .map(template -> new NotificationTemplateListItemVO(
                        template.getId(),
                        template.getNotifyScene(),
                        template.getChannelType(),
                        template.getTitle(),
                        template.getContentTemplate(),
                        template.getIsEnabled(),
                        template.getVersion(),
                        template.getUpdatedAt()))
                .toList();
        Page<NotificationTemplateListItemVO> voPage = new Page<>(
                templatePage.getCurrent(), templatePage.getSize(), templatePage.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public NotificationTemplateCreateVO createTemplate(NotificationTemplateCreateReq req) {
        // 校验场景与渠道枚举，保证与发送侧白名单一致。
        normalizeScene(req.getNotifyScene());
        normalizeChannel(req.getChannelType());

        // 同场景同渠道仅允许一条有效模板；应用层预检 + 唯一索引兜底。
        boolean exists = lambdaQuery()
                .eq(SysNotificationTemplate::getNotifyScene, req.getNotifyScene())
                .eq(SysNotificationTemplate::getChannelType, req.getChannelType())
                .exists();
        if (exists) {
            // TODO: ErrorCode 缺少 NOTIFICATION_TEMPLATE_ALREADY_EXISTS，暂以参数错误语义返回。
            throw new BusinessException(
                    ErrorCode.PARAM_VALID_ERROR, "同场景同渠道的模板已存在");
        }

        // 落库新模板：默认启用、版本 0。
        SysNotificationTemplate template = SysNotificationTemplate.builder()
                .notifyScene(req.getNotifyScene())
                .channelType(req.getChannelType())
                .title(req.getTitle())
                .contentTemplate(req.getContentTemplate())
                .isEnabled(true)
                .version(0)
                .build();
        try {
            save(template);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(
                    ErrorCode.PARAM_VALID_ERROR, "同场景同渠道的模板已存在");
        }
        // TODO: 通知实际发送侧接入后按模板占位符白名单校验 contentTemplate 中的变量。
        return new NotificationTemplateCreateVO(
                template.getId(),
                template.getNotifyScene(),
                template.getChannelType(),
                template.getVersion(),
                template.getCreatedAt());
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public NotificationTemplateUpdateVO updateTemplate(Long templateId,
                                                       NotificationTemplateUpdateReq req) {
        // 查询模板并校验期望版本。
        SysNotificationTemplate template = getById(templateId);
        if (template == null) {
            // TODO: ErrorCode 缺少 NOTIFICATION_TEMPLATE_NOT_FOUND，暂以参数错误语义返回。
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "通知模板不存在");
        }
        if (!Objects.equals(template.getVersion(), req.getExpectedVersion())) {
            // TODO: ErrorCode 缺少 NOTIFICATION_TEMPLATE_VERSION_CONFLICT，暂以参数错误语义返回。
            throw new BusinessException(
                    ErrorCode.PARAM_VALID_ERROR, "模板已被修改，请刷新后重试");
        }

        // 半量更新：实体承载已提交字段，@Version 乐观锁递增版本。
        SysNotificationTemplate update = new SysNotificationTemplate();
        update.setId(templateId);
        update.setVersion(req.getExpectedVersion());
        update.setTitle(req.getTitle());
        update.setContentTemplate(req.getContentTemplate());
        update.setIsEnabled(req.getEnabled());
        boolean updated = updateById(update);
        if (!updated) {
            throw new BusinessException(
                    ErrorCode.PARAM_VALID_ERROR, "模板已被修改，请刷新后重试");
        }

        // 回读最新版本与更新时间返回。
        SysNotificationTemplate latest = getById(templateId);
        return new NotificationTemplateUpdateVO(
                latest.getId(), latest.getVersion(), latest.getUpdatedAt());
    }

    private void validatePage(Integer page, Integer size) {
        if (page == null || page < 1 || size == null || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.PAGE_PARAM_INVALID);
        }
    }

    /**
     * 校验并返回合法的业务场景；空白入参返回 null 表示不过滤。
     */
    private String normalizeScene(String notifyScene) {
        if (notifyScene == null || notifyScene.isBlank()) {
            return null;
        }
        try {
            return NotifyScene.valueOf(notifyScene).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "不支持的业务场景");
        }
    }

    /**
     * 校验并返回合法的渠道类型；空白入参返回 null 表示不过滤。
     */
    private String normalizeChannel(String channelType) {
        if (channelType == null || channelType.isBlank()) {
            return null;
        }
        try {
            return ChannelType.valueOf(channelType).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "不支持的渠道类型");
        }
    }
}
