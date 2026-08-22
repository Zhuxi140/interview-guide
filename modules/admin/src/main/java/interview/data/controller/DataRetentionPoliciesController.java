package interview.data.controller;

import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import interview.data.model.req.DataRetentionPoliciesUpdateReq;
import interview.data.model.vo.DataRetentionPoliciesVO;
import interview.data.service.DataRetentionPolicyService;
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
 * 数据保留策略单行配置管理（Admin）。
 *
 * @author zhuxi
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/admin/data-retention-policies")
@Tag(name = "数据保留策略（Admin）")
@RequiredArgsConstructor
@Validated
public class DataRetentionPoliciesController {

    private final DataRetentionPolicyService dataRetentionPolicyService;

    @Operation(summary = "查询各类在线数据保留期限和归档开关")
    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(
            permissions = Perm.Audit.DATA_RETENTION_VIEW,
            scope = PermissionScope.PLATFORM
    )
    @GetMapping
    public Result<DataRetentionPoliciesVO> getPolicies() {
        return Result.success(dataRetentionPolicyService.getPolicies());
    }

    @Operation(summary = "按版本完整更新数据保留策略（CAS）")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(
            permissions = Perm.Audit.DATA_RETENTION_UPDATE,
            scope = PermissionScope.PLATFORM
    )
    @PutMapping
    public Result<DataRetentionPoliciesVO> updatePolicies(
            @Valid @RequestBody DataRetentionPoliciesUpdateReq req) {
        return Result.success(dataRetentionPolicyService.updatePolicies(req));
    }
}
