package interview.kyc.controller;

import interview.common.annonate.MaxRiskLevel;
import interview.common.constant.ApiVersion;
import interview.common.constant.Result;
import interview.common.enums.RiskLevel;
import interview.kyc.model.req.KycMaterialUploadTicketReq;
import interview.kyc.model.vo.KycMaterialUploadTicketVO;
import interview.kyc.model.vo.KycStatusVO;
import interview.kyc.service.KycService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zhuxi
 * @apiNote C 端个人实名认证（候选人自查，不加权限注解）
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/candidate/kyc")
@Tag(name = "个人实名认证（C端）")
@RequiredArgsConstructor
@Validated
public class CandidateKycController {

    private final KycService kycService;

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @Operation(summary = "获取本人 KYC 材料短期上传凭证")
    @PostMapping("/materials/upload-ticket")
    public Result<KycMaterialUploadTicketVO> createMaterialUploadTicket(
            @RequestBody @Valid KycMaterialUploadTicketReq req) {
        return Result.success(kycService.createMaterialUploadTicket(req));
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @Operation(summary = "查询本人实名认证审核状态")
    @GetMapping("/status")
    public Result<KycStatusVO> getMyKycStatus() {
        return Result.success(kycService.getMyKycStatus());
    }
}
