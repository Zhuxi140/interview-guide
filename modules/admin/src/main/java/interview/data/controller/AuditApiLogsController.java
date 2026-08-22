package interview.data.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import interview.data.model.vo.ApiLogListItemVO;
import interview.data.service.ApiLogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API 访问日志审计查询（Admin，仅 SUPER_ADMIN 可访问）。
 *
 * @author zhuxi
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/admin/audit")
@Tag(name = "API 访问日志审计（Admin）")
@RequiredArgsConstructor
@Validated
public class AuditApiLogsController {

    private final ApiLogQueryService apiLogQueryService;

    @Operation(summary = "平台查询 API 访问日志（分页）")
    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(
            permissions = Perm.Audit.API_LOGS_LIST,
            scope = PermissionScope.PLATFORM
    )
    @GetMapping("/api-logs")
    public Result<IPage<ApiLogListItemVO>> listApiLogs(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String apiPath,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(apiLogQueryService.pageApiLogs(
                page, size, userId, apiPath, method, status, startTime, endTime));
    }

    @Operation(summary = "平台查询单条 API 日志详情")
    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(
            permissions = Perm.Audit.API_LOGS_DETAIL,
            scope = PermissionScope.PLATFORM
    )
    @GetMapping("/api-logs/{id}")
    public Result<ApiLogListItemVO> getApiLogDetail(@PathVariable Long id) {
        return Result.success(apiLogQueryService.getApiLogDetail(id));
    }

    @Operation(summary = "查询归档 API 日志（分页）")
    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(
            permissions = Perm.Audit.API_LOGS_ARCHIVE,
            scope = PermissionScope.PLATFORM
    )
    @GetMapping("/api-log-archives")
    public Result<IPage<ApiLogListItemVO>> listApiLogArchives(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String apiPath,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(apiLogQueryService.pageApiLogArchives(
                page, size, userId, apiPath, method, status, startTime, endTime));
    }
}
