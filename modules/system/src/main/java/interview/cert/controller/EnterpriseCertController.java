package interview.cert.controller;

import interview.cert.model.req.EnterpriseCertSubmitReq;
import interview.cert.model.req.EnterpriseCertUploadTicketReq;
import interview.cert.model.vo.EnterpriseCertStatusVO;
import interview.cert.model.vo.EnterpriseCertSubmitVO;
import interview.cert.model.vo.EnterpriseCertUploadTicketVO;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zhuxi
 * @apiNote B 端企业资质认证（企业自查）
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/enterprises/{enterpriseId}/certification")
@Tag(name = "企业资质认证（B端）")
@RequiredArgsConstructor
@Validated
public class EnterpriseCertController {

    private final EnterpriseCertService enterpriseCertService;

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.EnterpriseCert.SUBMIT, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "获取营业执照短期上传凭证（OWNER/ADMIN）")
    @PostMapping("/materials/upload-ticket")
    public Result<EnterpriseCertUploadTicketVO> createLicenseUploadTicket(
            @PathVariable Long enterpriseId,
            @RequestBody @Valid EnterpriseCertUploadTicketReq req) {
        return Result.success(enterpriseCertService.createLicenseUploadTicket(enterpriseId, req));
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.EnterpriseCert.SUBMIT, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "提交企业资质认证（拒绝后可重报）")
    @PostMapping
    public Result<EnterpriseCertSubmitVO> submitCertification(
            @PathVariable Long enterpriseId,
            @RequestBody @Valid EnterpriseCertSubmitReq req) {
        return Result.success(enterpriseCertService.submitCertification(enterpriseId, req));
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.EnterpriseCert.STATUS, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "查询企业资质认证审核状态")
    @GetMapping("/status")
    public Result<EnterpriseCertStatusVO> getCertificationStatus(
            @PathVariable Long enterpriseId) {
        return Result.success(enterpriseCertService.getCertificationStatus(enterpriseId));
    }
}
