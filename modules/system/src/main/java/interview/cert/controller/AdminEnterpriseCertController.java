package interview.cert.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.cert.model.req.EnterpriseCertAuditReq;
import interview.cert.model.req.EnterpriseCertSearchReq;
import interview.cert.model.vo.EnterpriseCertAuditVO;
import interview.cert.model.vo.EnterpriseCertDetailVO;
import interview.cert.model.vo.EnterpriseCertLicensePresignVO;
import interview.cert.model.vo.EnterpriseCertListItemVO;
import interview.cert.service.EnterpriseCertService;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
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
 * @apiNote 平台端企业资质认证审核管理
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/admin/enterprise-certifications")
@Tag(name = "企业资质认证审核管理（平台）")
@RequiredArgsConstructor
@Validated
public class AdminEnterpriseCertController {

    private final EnterpriseCertService enterpriseCertService;

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.EnterpriseCert.ADMIN_AUDIT, scope = PermissionScope.PLATFORM)
    @Operation(summary = "原子审核企业资质（同事务联动企业状态）")
    @PostMapping("/{certId}/audit")
    public Result<EnterpriseCertAuditVO> auditCertification(
            @PathVariable Long certId,
            @RequestBody @Valid EnterpriseCertAuditReq req) {
        return Result.success(enterpriseCertService.auditCertification(certId, req));
    }

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.EnterpriseCert.ADMIN_LIST, scope = PermissionScope.PLATFORM)
    @Operation(summary = "分页查询企业认证审核列表")
    @GetMapping
    public Result<IPage<EnterpriseCertListItemVO>> pageCertifications(
            @Valid @ParameterObject EnterpriseCertSearchReq req) {
        return Result.success(enterpriseCertService.pageCertifications(req));
    }

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.EnterpriseCert.ADMIN_LIST, scope = PermissionScope.PLATFORM)
    @Operation(summary = "查询企业认证审核详情（敏感字段脱敏）")
    @GetMapping("/{certId}")
    public Result<EnterpriseCertDetailVO> getCertificationDetail(@PathVariable Long certId) {
        return Result.success(enterpriseCertService.getCertificationDetail(certId));
    }

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.EnterpriseCert.ADMIN_LIST, scope = PermissionScope.PLATFORM)
    @Operation(summary = "获取营业执照短期审核地址")
    @GetMapping("/{certId}/license")
    public Result<EnterpriseCertLicensePresignVO> getLicensePresign(@PathVariable Long certId) {
        return Result.success(enterpriseCertService.getLicensePresign(certId));
    }
}
