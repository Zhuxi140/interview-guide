package interview.data.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import interview.data.model.vo.OperateLogDetailVO;
import interview.data.model.vo.OperateLogListItemVO;
import interview.data.service.OperateLogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 平台业务操作审计查询（Admin，仅 SUPER_ADMIN 可访问）。
 *
 * @author zhuxi
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/admin/audit/operate-logs")
@Tag(name = "操作审计日志（Admin）")
@RequiredArgsConstructor
@Validated
public class AuditOperateLogsController {

    private final OperateLogQueryService operateLogQueryService;

    @Operation(summary = "平台查询操作审计日志（分页）")
    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(
            permissions = Perm.Audit.OPERATE_LOGS_LIST,
            scope = PermissionScope.PLATFORM
    )
    @GetMapping
    public Result<IPage<OperateLogListItemVO>> listOperateLogs(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operateType,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(operateLogQueryService.pageOperateLogs(
                page, size, userId, module, operateType, resourceType, resourceId,
                startTime, endTime));
    }

    @Operation(summary = "平台查询单条脱敏操作审计详情")
    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(
            permissions = Perm.Audit.OPERATE_LOGS_DETAIL,
            scope = PermissionScope.PLATFORM
    )
    @GetMapping("/{id}")
    public Result<OperateLogDetailVO> getOperateLogDetail(@PathVariable Long id) {
        return Result.success(operateLogQueryService.getOperateLogDetail(id));
    }

    @Operation(summary = "按 traceId 查询全部脱敏操作变更")
    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(
            permissions = Perm.Audit.OPERATE_LOGS_TRACE,
            scope = PermissionScope.PLATFORM
    )
    @GetMapping("/trace/{traceId}")
    public Result<List<OperateLogDetailVO>> listByTraceId(
            @PathVariable @NotBlank String traceId) {
        return Result.success(operateLogQueryService.listByTraceId(traceId));
    }
}
