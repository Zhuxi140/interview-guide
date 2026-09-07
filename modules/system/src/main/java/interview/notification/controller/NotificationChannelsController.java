package interview.notification.controller;

import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import interview.notification.model.req.NotificationChannelsUpdateReq;
import interview.notification.model.vo.NotificationChannelsVO;
import interview.notification.service.NotificationChannelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知发送渠道配置管理（Admin）。
 *
 * @author zhuxi
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/admin/notification-channels")
@Tag(name = "通知渠道配置（Admin）")
@RequiredArgsConstructor
@Validated
public class NotificationChannelsController {

    private final NotificationChannelService notificationChannelService;

    @Operation(summary = "查询邮件/短信/站内信渠道配置与启用状态")
    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(
            permissions = Perm.NotificationAdmin.CHANNEL_VIEW,
            scope = PermissionScope.PLATFORM
    )
    @GetMapping
    public Result<NotificationChannelsVO> getChannels() {
        return Result.success(notificationChannelService.getChannels());
    }

    @Operation(summary = "按版本完整更新渠道配置（CAS，凭证加密落盘）")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(
            permissions = Perm.NotificationAdmin.CHANNEL_UPDATE,
            scope = PermissionScope.PLATFORM
    )
    @PutMapping
    public Result<NotificationChannelsVO> updateChannels(
            @Valid @RequestBody NotificationChannelsUpdateReq req) {
        return Result.success(notificationChannelService.updateChannels(req));
    }
}
