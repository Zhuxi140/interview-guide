package interview.notification.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.annonate.MaxRiskLevel;
import interview.common.constant.ApiVersion;
import interview.common.constant.Result;
import interview.common.enums.RiskLevel;
import interview.notification.model.req.NotificationBatchReadReq;
import interview.notification.model.req.NotificationReadReq;
import interview.notification.model.vo.NotificationBatchReadVO;
import interview.notification.model.vo.NotificationListItemVO;
import interview.notification.model.vo.NotificationReadVO;
import interview.notification.model.vo.NotificationUnreadCountVO;
import interview.notification.service.NotificationService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 我的消息（收件侧自查，全体登录用户可用，无企业域）。
 *
 * @author zhuxi
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/notifications")
@Tag(name = "我的消息通知")
@RequiredArgsConstructor
@Validated
public class NotificationsController {

    private final NotificationService notificationService;

    @Operation(summary = "查询我的消息列表（分页）")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @GetMapping
    public Result<IPage<NotificationListItemVO>> listMyNotifications(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) String notifyType,
            @RequestParam(required = false) Boolean isRead) {
        return Result.success(notificationService.pageMyNotifications(
                page, size, notifyType, isRead));
    }

    @Operation(summary = "查询我的未读消息数量")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @GetMapping("/unread-count")
    public Result<NotificationUnreadCountVO> getUnreadCount() {
        return Result.success(notificationService.getUnreadCount());
    }

    @Operation(summary = "标记单条消息为已读")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @PatchMapping("/{id}")
    public Result<NotificationReadVO> markRead(
            @PathVariable("id") Long notificationId,
            @Valid @RequestBody NotificationReadReq req) {
        return Result.success(
                notificationService.markRead(notificationId, req.getIsRead()));
    }

    @Operation(summary = "批量按筛选标记消息已读")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @PatchMapping
    public Result<NotificationBatchReadVO> markAllRead(
            @Valid @RequestBody NotificationBatchReadReq req) {
        return Result.success(notificationService.markAllRead(req));
    }
}
