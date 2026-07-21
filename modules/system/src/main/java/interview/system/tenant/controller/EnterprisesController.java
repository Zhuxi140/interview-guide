package interview.system.tenant.controller;

import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.constant.SecureActionContext;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import interview.common.enums.SecureActionType;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequirePermission;
import interview.common.annonate.RequireSecure;
import interview.system.tenant.EnterpriseConverter;
import interview.system.tenant.model.bo.EnterpriseCreateBO;
import interview.system.tenant.model.bo.ListUserEnterprisesBO;
import interview.system.tenant.model.req.EnterpriseBasicUpdateReq;
import interview.system.tenant.model.req.EnterpriseContactCodeVerifyReq;
import interview.system.tenant.model.req.EnterpriseContactEmailUpdateReq;
import interview.system.tenant.model.req.EnterpriseContactNewPhoneReq;
import interview.system.tenant.model.req.EnterpriseCreateReq;
import interview.system.tenant.model.vo.EnterpriseCreateVO;
import interview.system.tenant.model.vo.EnterpriseContactEmailUpdateVO;
import interview.system.tenant.model.vo.EnterpriseContactPhoneUpdateVO;
import interview.system.tenant.model.vo.EnterpriseDetailVO;
import interview.system.tenant.model.vo.EnterpriseListItemVO;
import interview.system.tenant.model.vo.EnterpriseContactVerifyStartVO;
import interview.system.tenant.model.vo.EnterpriseUpdateVO;
import interview.system.auth.model.vo.SecureChallengeStartVO;
import interview.system.tenant.service.EnterpriseContactVerificationService;
import interview.system.tenant.service.EnterprisesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    private final EnterpriseContactVerificationService contactVerificationService;
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

    @MaxRiskLevel(RiskLevel.MID_RISK)
    @RequirePermission(permissions = Perm.Enterprise.UPDATE_CONTACT, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "启动企业联系电话变更验证流程")
    @PostMapping("/{enterpriseId}/contact-verification")
    public Result<EnterpriseContactVerifyStartVO> startContactVerification(
            @PathVariable("enterpriseId") Long enterpriseId) {
        // 创建企业联系电话验证流程并返回是否需要验证原号码
        return Result.success(contactVerificationService.start(enterpriseId));
    }

    @MaxRiskLevel(RiskLevel.MID_RISK)
    @RequirePermission(permissions = Perm.Enterprise.UPDATE_CONTACT, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "验证企业原联系电话")
    @PostMapping("/{enterpriseId}/contact-verification/old/verify")
    public Result<Void> verifyOldContactPhone(
            @PathVariable("enterpriseId") Long enterpriseId,
            @RequestBody @Valid EnterpriseContactCodeVerifyReq req) {
        // 核销企业原联系电话验证码并推进流程状态
        contactVerificationService.verifyOldPhone(enterpriseId, req);
        return Result.success();
    }

    @MaxRiskLevel(RiskLevel.MID_RISK)
    @RequirePermission(permissions = Perm.Enterprise.UPDATE_CONTACT, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "发送企业新联系电话验证码")
    @PostMapping("/{enterpriseId}/contact-verification/new/send")
    public Result<Void> sendNewContactPhoneCode(
            @PathVariable("enterpriseId") Long enterpriseId,
            @RequestBody @Valid EnterpriseContactNewPhoneReq req) {
        // 将新联系电话绑定到流程并发送验证码
        contactVerificationService.sendNewPhoneCode(enterpriseId, req);
        return Result.success();
    }

    @MaxRiskLevel(RiskLevel.MID_RISK)
    @RequirePermission(permissions = Perm.Enterprise.UPDATE_CONTACT, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "验证企业新联系电话并签发安全操作令牌")
    @PostMapping("/{enterpriseId}/contact-verification/new/verify")
    public Result<String> verifyNewContactPhone(
            @PathVariable("enterpriseId") Long enterpriseId,
            @RequestBody @Valid EnterpriseContactCodeVerifyReq req) {
        // 验证新联系电话并返回一次性安全操作令牌
        return Result.success(contactVerificationService.verifyNewPhone(enterpriseId, req));
    }

    @MaxRiskLevel(RiskLevel.MID_RISK)
    @RequirePermission(permissions = Perm.Enterprise.UPDATE_CONTACT, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "创建企业联系邮箱更新 Challenge")
    @PostMapping("/{enterpriseId}/contact/email/challenge")
    public Result<SecureChallengeStartVO> startContactEmailChallenge(
            @PathVariable("enterpriseId") Long enterpriseId) {
        // 服务端固定邮箱更新动作、目标企业和当前用户绑定手机号
        return Result.success(
                enterprisesService.startContactEmailChallenge(enterpriseId));
    }

    @MaxRiskLevel(RiskLevel.MID_RISK)
    @RequirePermission(permissions = Perm.Enterprise.DELETE, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "创建企业注销 Challenge")
    @PostMapping("/{enterpriseId}/deletion/challenge")
    public Result<SecureChallengeStartVO> startDeletionChallenge(
            @PathVariable("enterpriseId") Long enterpriseId) {
        // 服务端固定企业注销动作、目标企业和当前用户绑定手机号
        return Result.success(enterprisesService.startDeletionChallenge(enterpriseId));
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.Enterprise.UPDATE_CONTACT, scope = PermissionScope.ENTERPRISE)
    @RequireSecure(SecureActionType.UPDATE_ENTERPRISE_EMAIL)
    @Operation(summary = "更新企业联系邮箱，需携带邮箱更新令牌")
    @PutMapping("/{enterpriseId}/contact/email")
    public Result<EnterpriseContactEmailUpdateVO> updateEnterpriseContactEmail(
            @PathVariable("enterpriseId") Long enterpriseId,
            @RequestBody @Valid EnterpriseContactEmailUpdateReq req) {
        // 读取拦截器已校验并消费的邮箱更新安全上下文
        SecureActionContext context = (SecureActionContext) request.getAttribute(
                SecureActionContext.REQUEST_ATTRIBUTE);
        return Result.success(enterprisesService.updateEnterpriseContactEmail(
                enterpriseId, context, req));
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.Enterprise.UPDATE_CONTACT, scope = PermissionScope.ENTERPRISE)
    @RequireSecure(SecureActionType.UPDATE_ENTERPRISE_PHONE)
    @Operation(summary = "完成企业联系电话变更，需携带企业新号码验证令牌")
    @PutMapping("/{enterpriseId}/contact/phone")
    public Result<EnterpriseContactPhoneUpdateVO> updateEnterpriseContactPhone(
            @PathVariable("enterpriseId") Long enterpriseId) {
        // 读取拦截器已校验并消费的安全操作上下文
        SecureActionContext secureActionContext = (SecureActionContext) request.getAttribute(
                SecureActionContext.REQUEST_ATTRIBUTE);

        // 使用令牌中的新旧号码快照更新联系电话，成功后清理验证流程
        EnterpriseContactPhoneUpdateVO vo = enterprisesService.updateEnterpriseContactPhone(
                enterpriseId, secureActionContext);
        contactVerificationService.complete(secureActionContext.getChallengeId());
        return Result.success(vo);
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.Enterprise.DELETE, scope = PermissionScope.ENTERPRISE)
    @RequireSecure(SecureActionType.DELETE_ENTERPRISE)
    @Operation(summary = "注销企业（仅 OWNER可操作）")
    @DeleteMapping("/{enterpriseId}")
    public Result<Void> deleteEnterprise(@PathVariable("enterpriseId") Long enterpriseId) {
        // 读取拦截器已校验并消费的企业注销安全上下文
        SecureActionContext context = (SecureActionContext) request.getAttribute(
                SecureActionContext.REQUEST_ATTRIBUTE);
        enterprisesService.deleteEnterprise(enterpriseId, context);
        return Result.success();
    }

}
