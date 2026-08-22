package interview.notification.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import interview.notification.model.vo.NotificationSendRecordListItemVO;
import interview.notification.service.NotificationSendRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 全站通知发送记录查询（Admin）。
 *
 * @author zhuxi
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/admin/notifications")
@Tag(name = "通知发送记录（Admin）")
@RequiredArgsConstructor
@Validated
public class AdminNotificationsController {

    private final NotificationSendRecordService notificationSendRecordService;

    @Operation(summary = "查询全站通知发送记录（分页）")
    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(
            permissions = Perm.NotificationAdmin.SEND_LIST,
            scope = PermissionScope.PLATFORM
    )
    @GetMapping
    public Result<IPage<NotificationSendRecordListItemVO>> listSendRecords(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) String channelType,
            @RequestParam(required = false) String sendStatus,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(notificationSendRecordService.pageRecords(
                page, size, channelType, sendStatus, startTime, endTime));
    }
}
