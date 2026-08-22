package interview.interviewcfg.calendar.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequireActiveEnterprise;
import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import interview.interviewcfg.calendar.model.enums.CalendarSlotStatus;
import interview.interviewcfg.calendar.model.vo.CalendarSlotListItemVO;
import interview.interviewcfg.calendar.service.CalendarSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 面试官日历空闲时段管理（B端）。
 *
 * @author zhuxi
 */
@RestController
@RequireActiveEnterprise
@RequestMapping(ApiVersion.V1 + "/enterprises/{enterpriseId}/calendar-slots")
@Tag(name = "日历空闲时段（B端）")
@RequiredArgsConstructor
@Validated
public class CalendarSlotsController {

    private final CalendarSlotService calendarSlotService;

    @Operation(summary = "查询面试官日历空闲时段（分页）")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(
            permissions = Perm.CalendarSlots.LIST,
            scope = PermissionScope.ENTERPRISE
    )
    @GetMapping
    public Result<IPage<CalendarSlotListItemVO>> listSlots(
            @PathVariable Long enterpriseId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) Long interviewerUserId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) CalendarSlotStatus status) {
        return Result.success(calendarSlotService.pageSlots(
                enterpriseId, page, size, interviewerUserId, startTime, endTime, status));
    }

    @Operation(summary = "删除本人尚未占用的空闲时段")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(
            permissions = Perm.CalendarSlots.DELETE,
            scope = PermissionScope.ENTERPRISE
    )
    @DeleteMapping("/{slotId}")
    public Result<Void> deleteMySlot(
            @PathVariable Long enterpriseId,
            @PathVariable Long slotId,
            @RequestHeader("If-Match")
            @Parameter(name = "If-Match", description = "时段当前版本号，用于乐观锁删除校验",
                    required = true, example = "0")
            @Min(value = 0, message = "版本号不能小于 0")
            Integer expectedVersion) {
        calendarSlotService.deleteMySlot(enterpriseId, slotId, expectedVersion);
        return Result.success();
    }
}
