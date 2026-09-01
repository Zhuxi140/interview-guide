package interview.candidate.controller;

import interview.candidate.model.req.CandidateProfileUpdateReq;
import interview.candidate.model.vo.CandidateBasicProfileVO;
import interview.candidate.model.vo.CandidateProfileUpdateVO;
import interview.candidate.service.CandidateBasicProfileService;
import interview.common.annonate.MaxRiskLevel;
import interview.common.constant.ApiVersion;
import interview.common.constant.Result;
import interview.common.enums.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiVersion.V1 + "/candidate/profile")
@Tag(name = "候选人资料（C端）")
@RequiredArgsConstructor
public class CandidateProfileController {

    private final CandidateBasicProfileService candidateBasicProfileService;

    @Operation(summary = "查询当前候选人的可编辑资料和能力画像摘要")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @GetMapping
    public Result<CandidateBasicProfileVO> getMyProfile() {
        return Result.success(candidateBasicProfileService.getMyProfile());
    }

    @Operation(summary = "按版本部分更新当前候选人资料")
    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @PatchMapping
    public Result<CandidateProfileUpdateVO> updateMyProfile(
            @Valid @RequestBody CandidateProfileUpdateReq req) {
        return Result.success(candidateBasicProfileService.updateMyProfile(req));
    }
}
