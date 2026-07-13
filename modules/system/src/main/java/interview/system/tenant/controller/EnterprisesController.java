package interview.system.tenant.controller;

import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.constant.SecureActionContext;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import interview.common.enums.SmsType;
import interview.framework.annonate.MaxRiskLevel;
import interview.framework.annonate.RequirePermission;
import interview.framework.annonate.RequireSecure;
import interview.system.tenant.EnterpriseConverter;
import interview.system.tenant.model.bo.EnterpriseCreateBO;
import interview.system.tenant.model.bo.ListUserEnterprisesBO;
import interview.system.tenant.model.req.EnterpriseBasicUpdateReq;
import interview.system.tenant.model.req.EnterpriseContactUpdateReq;
import interview.system.tenant.model.req.EnterpriseCreateReq;
import interview.system.tenant.model.vo.EnterpriseCreateVO;
import interview.system.tenant.model.vo.EnterpriseDetailVO;
import interview.system.tenant.model.vo.EnterpriseListItemVO;
import interview.system.tenant.model.vo.EnterpriseContactUpdateVO;
import interview.system.tenant.model.vo.EnterpriseUpdateVO;
import interview.system.tenant.service.EnterprisesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author zhuxi
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/enterprises")
@Tag(name = "企业管理")
@RequiredArgsConstructor
public class EnterprisesController {
    private final EnterprisesService enterprisesService;
    private final EnterpriseConverter converter;
    private final HttpServletRequest request;

    @MaxRiskLevel(RiskLevel.MID_RISK)
    @Operation(summary = "创建企业（创建者自动成为 OWNER）")
    @PostMapping
    public Result<EnterpriseCreateVO> createEnterprise(@RequestBody @Valid EnterpriseCreateReq req) {
        EnterpriseCreateBO enterprise = enterprisesService.createEnterprise(req);
        EnterpriseCreateVO vo = converter.convertToEnterpriseCreateVO(enterprise);
        return Result.success(vo);
    }

    @MaxRiskLevel(RiskLevel.HIGH_RISK)
    @RequirePermission(permissions = Perm.Enterprise.LIST, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "查询当前用户所属企业列表")
    @GetMapping
    public Result<List<EnterpriseListItemVO>> getUserListEnterprises() {
        List<ListUserEnterprisesBO> listUserEnterprisesBOS = enterprisesService.listUserEnterprises();
        List<EnterpriseListItemVO> vos = converter.BOCovertToEnterpriseListItemVO(listUserEnterprisesBOS);
        return Result.success(vos);
    }

    @MaxRiskLevel(RiskLevel.HIGH_RISK)
    @RequirePermission(permissions = Perm.Enterprise.DETAIL, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "查询企业详情")
    @GetMapping("/{enterpriseId}")
    public Result<EnterpriseDetailVO> getEnterprisesDetail(@PathVariable("enterpriseId") Long enterpriseId) {
        EnterpriseDetailVO vo = enterprisesService.getEnterpriseDetail(enterpriseId);
        return Result.success(vo);
    }

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.Enterprise.UPDATE, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "更新企业基本信息（名称、简称、行业、规模、Logo）")
    @PatchMapping("/{enterpriseId}")
    public Result<EnterpriseUpdateVO> updateEnterpriseBasic(@PathVariable("enterpriseId") Long enterpriseId,
                                                            @RequestBody @Valid EnterpriseBasicUpdateReq req) {
        EnterpriseUpdateVO vo = enterprisesService.updateEnterpriseBasic(enterpriseId, req);
        return Result.success(vo);
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.Enterprise.UPDATE_CONTACT, scope = PermissionScope.ENTERPRISE)
    @RequireSecure(allowList = {SmsType.SENSITIVE_OPERATION,SmsType.DOUBLE_VERIFY_NEW})
    @Operation(summary = "更新企业联系方式（邮箱、手机），需携带 X-Secure-Action-Token")
    @PutMapping("/{enterpriseId}/contact")
    public Result<EnterpriseContactUpdateVO> updateEnterpriseContact(@PathVariable("enterpriseId") Long enterpriseId,
                                                                     @RequestBody @Valid EnterpriseContactUpdateReq req) {
        SecureActionContext secureActionContext = (SecureActionContext)request.getAttribute("SECURE_ACTION_CONTEXT");
        EnterpriseContactUpdateVO vo = enterprisesService.updateEnterpriseContact(enterpriseId,secureActionContext, req);
        return Result.success(vo);
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.Enterprise.DELETE, scope = PermissionScope.ENTERPRISE)
    @RequireSecure(allowList = {SmsType.SENSITIVE_OPERATION})
    @Operation(summary = "注销企业（仅 OWNER可操作）")
    @DeleteMapping("/{enterpriseId}")
    public Result<Void> deleteEnterprise(@PathVariable("enterpriseId") Long enterpriseId) {
        enterprisesService.deleteEnterprise(enterpriseId);
        return Result.success();
    }

}
