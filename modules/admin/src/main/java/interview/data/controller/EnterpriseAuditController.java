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
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业操作审计查询（企业管理员查看本企业脱敏操作日志）。
 *
 * @author zhuxi
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/enterprises/{enterpriseId}/audit/operate-logs")
@Tag(name = "操作审计日志（企业域）")
@RequiredArgsConstructor
@Validated
public class EnterpriseAuditController {

    private final OperateLogQueryService operateLogQueryService;

    @Operation(summary = "企业管理员查询本企业操作日志（分页）")
    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(
            permissions = Perm.Audit.ENTERPRISE_OPERATE_LOGS_LIST,
            scope = PermissionScope.ENTERPRISE
    )
    @GetMapping
    public Result<IPage<OperateLogListItemVO>> listEnterpriseOperateLogs(
            @PathVariable Long enterpriseId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operateType,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(operateLogQueryService.pageEnterpriseOperateLogs(
                enterpriseId, page, size, module, operateType, resourceType, resourceId,
                startTime, endTime));
    }

    @Operation(summary = "企业管理员查询本企业单条审计详情")
    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(
            permissions = Perm.Audit.ENTERPRISE_OPERATE_LOGS_DETAIL,
            scope = PermissionScope.ENTERPRISE
    )
    @GetMapping("/{id}")
    public Result<OperateLogDetailVO> getEnterpriseOperateLogDetail(
            @PathVariable Long enterpriseId,
            @PathVariable Long id) {
        return Result.success(
                operateLogQueryService.getEnterpriseOperateLogDetail(enterpriseId, id));
    }
}
