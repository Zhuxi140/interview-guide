package interview.kyc.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import interview.kyc.model.enums.KycMaterialType;
import interview.kyc.model.req.KycAuditReq;
import interview.kyc.model.req.KycAuditSearchReq;
import interview.kyc.model.vo.KycAuditVO;
import interview.kyc.model.vo.KycDetailVO;
import interview.kyc.model.vo.KycListItemVO;
import interview.kyc.model.vo.KycMaterialPresignVO;
import interview.kyc.service.KycService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zhuxi
 * @apiNote 平台端实名认证审核管理
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/admin/kyc")
@Tag(name = "实名认证审核管理（平台）")
@RequiredArgsConstructor
@Validated
public class AdminKycController {

    private final KycService kycService;

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.Kyc.ADMIN_AUDIT, scope = PermissionScope.PLATFORM)
    @Operation(summary = "原子审核个人实名（仅允许 待审核 → 通过/拒绝）")
    @PostMapping("/{kycId}/audit")
    public Result<KycAuditVO> auditKyc(
            @PathVariable Long kycId,
            @RequestBody @Valid KycAuditReq req) {
        return Result.success(kycService.auditKyc(kycId, req));
    }

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.Kyc.ADMIN_LIST, scope = PermissionScope.PLATFORM)
    @Operation(summary = "分页查询实名认证审核列表")
    @GetMapping
    public Result<IPage<KycListItemVO>> pageKyc(@Valid @ParameterObject KycAuditSearchReq req) {
        return Result.success(kycService.pageKyc(req));
    }

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.Kyc.ADMIN_LIST, scope = PermissionScope.PLATFORM)
    @Operation(summary = "查询个人实名审核详情（敏感字段脱敏）")
    @GetMapping("/{kycId}")
    public Result<KycDetailVO> getKycDetail(@PathVariable Long kycId) {
        return Result.success(kycService.getKycDetail(kycId));
    }

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.Kyc.ADMIN_LIST, scope = PermissionScope.PLATFORM)
    @Operation(summary = "获取个人实名材料短期审核地址")
    @GetMapping("/{kycId}/materials/{materialType}")
    public Result<KycMaterialPresignVO> getMaterialPresign(
            @PathVariable Long kycId,
            @PathVariable @Parameter(description = "材料类型", example = "ID_CARD_FRONT")
            KycMaterialType materialType) {
        return Result.success(kycService.getMaterialPresign(kycId, materialType));
    }
}
