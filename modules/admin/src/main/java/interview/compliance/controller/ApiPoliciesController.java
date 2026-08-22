package interview.compliance.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import interview.compliance.model.req.ApiPolicyCreateReq;
import interview.compliance.model.req.ApiPolicySearchReq;
import interview.compliance.model.req.ApiPolicyUpdateReq;
import interview.compliance.model.vo.ApiPolicyListItemVO;
import interview.compliance.model.vo.ApiPolicyMutationVO;
import interview.compliance.service.ApiPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zhuxi
 * @apiNote 平台端用户 API 策略管理（限流/黑白名单）
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/admin/api-policies")
@Tag(name = "用户 API 策略管理（平台）")
@RequiredArgsConstructor
@Validated
public class ApiPoliciesController {

    private final ApiPolicyService apiPolicyService;

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.ApiPolicies.CREATE, scope = PermissionScope.PLATFORM)
    @Operation(summary = "创建用户 API 策略（限流/风控名单）")
    @PostMapping
    public Result<ApiPolicyMutationVO> createPolicy(@RequestBody @Valid ApiPolicyCreateReq req) {
        return Result.success(apiPolicyService.createPolicy(req));
    }

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.ApiPolicies.LIST, scope = PermissionScope.PLATFORM)
    @Operation(summary = "分页查询用户 API 策略列表")
    @GetMapping
    public Result<IPage<ApiPolicyListItemVO>> pagePolicies(
            @Valid @ParameterObject ApiPolicySearchReq req) {
        return Result.success(apiPolicyService.pagePolicies(req));
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.ApiPolicies.UPDATE, scope = PermissionScope.PLATFORM)
    @Operation(summary = "更新用户 API 策略（半量更新 + 乐观锁）")
    @PatchMapping("/{policyId}")
    public Result<ApiPolicyMutationVO> updatePolicy(
            @PathVariable Long policyId,
            @RequestBody @Valid ApiPolicyUpdateReq req) {
        return Result.success(apiPolicyService.updatePolicy(policyId, req));
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.ApiPolicies.DELETE, scope = PermissionScope.PLATFORM)
    @Operation(summary = "删除用户 API 策略（逻辑删除 + If-Match 版本校验）")
    @DeleteMapping("/{policyId}")
    public Result<Void> deletePolicy(
            @PathVariable Long policyId,
            @RequestHeader("If-Match")
            @Parameter(name = "If-Match", description = "策略当前版本号，用于乐观锁删除校验",
                    required = true, example = "0")
            @Min(value = 0, message = "版本号不能小于 0")
            Integer expectedVersion) {
        apiPolicyService.deletePolicy(policyId, expectedVersion);
        return Result.success();
    }
}
