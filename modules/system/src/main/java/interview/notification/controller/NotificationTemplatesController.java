package interview.notification.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import interview.notification.model.req.NotificationTemplateCreateReq;
import interview.notification.model.req.NotificationTemplateUpdateReq;
import interview.notification.model.vo.NotificationTemplateCreateVO;
import interview.notification.model.vo.NotificationTemplateListItemVO;
import interview.notification.model.vo.NotificationTemplateUpdateVO;
import interview.notification.service.NotificationTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知模板管理（Admin）。
 *
 * @author zhuxi
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/admin/notification-templates")
@Tag(name = "通知模板管理（Admin）")
@RequiredArgsConstructor
@Validated
public class NotificationTemplatesController {

    private final NotificationTemplateService notificationTemplateService;

    @Operation(summary = "查询通知模板列表（分页）")
    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(
            permissions = Perm.NotificationAdmin.TEMPLATE_LIST,
            scope = PermissionScope.PLATFORM
    )
    @GetMapping
    public Result<IPage<NotificationTemplateListItemVO>> listTemplates(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) String notifyScene,
            @RequestParam(required = false) String channelType) {
        return Result.success(
                notificationTemplateService.pageTemplates(page, size, notifyScene, channelType));
    }

    @Operation(summary = "创建通知模板")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(
            permissions = Perm.NotificationAdmin.TEMPLATE_CREATE,
            scope = PermissionScope.PLATFORM
    )
    @PostMapping
    public Result<NotificationTemplateCreateVO> createTemplate(
            @Valid @RequestBody NotificationTemplateCreateReq req) {
        return Result.success(notificationTemplateService.createTemplate(req));
    }

    @Operation(summary = "编辑通知模板（携带 expectedVersion）")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(
            permissions = Perm.NotificationAdmin.TEMPLATE_UPDATE,
            scope = PermissionScope.PLATFORM
    )
    @PatchMapping("/{templateId}")
    public Result<NotificationTemplateUpdateVO> updateTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody NotificationTemplateUpdateReq req) {
        return Result.success(notificationTemplateService.updateTemplate(templateId, req));
    }
}
