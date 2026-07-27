package interview.billing.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.billing.model.vo.TokenConsumeLogListItemVO;
import interview.billing.service.TokenConsumeLogService;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.BizType;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping(ApiVersion.V1 + "/enterprises/{enterpriseId}/billing/token-consume-logs")
@Tag(name = "算力消耗流水（企业端）")
@RequiredArgsConstructor
@Validated
public class TokenConsumeLogController {

    private final TokenConsumeLogService tokenConsumeLogService;

    @Operation(summary = "查询算力消耗明细（分页）")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.BillingConsumeLog.LIST, scope = PermissionScope.ENTERPRISE)
    @GetMapping
    public Result<IPage<TokenConsumeLogListItemVO>> listLogs(
            @PathVariable Long enterpriseId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) BizType bizType,
            @RequestParam(required = false) OffsetDateTime startTime,
            @RequestParam(required = false) OffsetDateTime endTime) {
        return Result.success(tokenConsumeLogService.pageLogs(
                enterpriseId, page, size, bizType, startTime, endTime));
    }
}
