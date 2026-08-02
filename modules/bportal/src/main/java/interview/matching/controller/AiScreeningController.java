package interview.matching.controller;

import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequirePermission;
import interview.common.annonate.RequireActiveEnterprise;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import interview.matching.model.req.ApplicationAiReviewReq;
import interview.matching.model.req.JobScreeningConfigUpdateReq;
import interview.matching.model.vo.ApplicationAiScreeningReviewVO;
import interview.matching.model.vo.ApplicationAiScreeningTriggerVO;
import interview.matching.model.vo.ApplicationAiScreeningVO;
import interview.matching.model.vo.JobScreeningConfigVO;
import interview.matching.service.ApplicationAiScreeningService;
import interview.matching.service.JobScreeningConfigService;
import interview.resume.model.vo.CandidateProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * HR 岗位初筛配置、AI 建议与人工审核接口。
 */
@RestController
@RequireActiveEnterprise
@RequestMapping(ApiVersion.V1 + "/enterprises/{enterpriseId}")
@Tag(name = "HR AI 初筛")
@RequiredArgsConstructor
@Validated
public class AiScreeningController {

    private final JobScreeningConfigService jobScreeningConfigService;
    private final ApplicationAiScreeningService applicationAiScreeningService;

    @RequirePermission(permissions = Perm.Application.SCREENING_CONFIG_DETAIL,
            scope = PermissionScope.ENTERPRISE)
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @Operation(summary = "查询岗位 AI 初筛配置")
    @GetMapping("/jobs/{jobId}/screening-config")
    public Result<JobScreeningConfigVO> getScreeningConfig(
            @PathVariable("enterpriseId") Long enterpriseId,
            @PathVariable("jobId") Long jobId) {
        return Result.success(jobScreeningConfigService.getConfig(enterpriseId, jobId));
    }

    @RequirePermission(permissions = Perm.Application.SCREENING_CONFIG_UPDATE,
            scope = PermissionScope.ENTERPRISE)
    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @Operation(summary = "创建或更新岗位 AI 初筛配置")
    @PutMapping("/jobs/{jobId}/screening-config")
    public Result<JobScreeningConfigVO> saveScreeningConfig(
            @PathVariable("enterpriseId") Long enterpriseId,
            @PathVariable("jobId") Long jobId,
            @RequestBody @Valid JobScreeningConfigUpdateReq req) {
        return Result.success(
                jobScreeningConfigService.saveConfig(enterpriseId, jobId, req));
    }

    @RequirePermission(permissions = Perm.Application.AI_SCREENING_CREATE,
            scope = PermissionScope.ENTERPRISE)
    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @Operation(summary = "发起 HR AI 初筛")
    @PostMapping("/applications/{applicationId}/ai-screenings")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Result<ApplicationAiScreeningTriggerVO> createScreening(
            @PathVariable("enterpriseId") Long enterpriseId,
            @PathVariable("applicationId") Long applicationId,
            @RequestHeader("Idempotency-Key")
            @NotBlank @Size(max = 128) String idempotencyKey) {
        return Result.success(applicationAiScreeningService.accept(
                enterpriseId, applicationId, idempotencyKey));
    }

    @RequirePermission(permissions = Perm.Application.AI_SCREENING_DETAIL,
            scope = PermissionScope.ENTERPRISE)
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @Operation(summary = "查询最新 HR AI 初筛结果")
    @GetMapping("/applications/{applicationId}/ai-screenings/latest")
    public Result<ApplicationAiScreeningVO> getLatestScreening(
            @PathVariable("enterpriseId") Long enterpriseId,
            @PathVariable("applicationId") Long applicationId) {
        return Result.success(applicationAiScreeningService.getLatest(
                enterpriseId, applicationId));
    }

    @RequirePermission(permissions = Perm.Application.CANDIDATE_PROFILE_DETAIL,
            scope = PermissionScope.ENTERPRISE)
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @Operation(summary = "查询投递使用的人才画像")
    @GetMapping("/applications/{applicationId}/candidate-profile")
    public Result<CandidateProfileVO> getCandidateProfile(
            @PathVariable("enterpriseId") Long enterpriseId,
            @PathVariable("applicationId") Long applicationId) {
        return Result.success(applicationAiScreeningService.getCandidateProfile(
                enterpriseId, applicationId));
    }

    @RequirePermission(permissions = Perm.Application.AI_SCREENING_REVIEW,
            scope = PermissionScope.ENTERPRISE)
    @MaxRiskLevel(RiskLevel.MID_RISK)
    @Operation(summary = "HR 确认或推翻 AI 初筛建议")
    @PostMapping("/applications/{applicationId}/ai-screenings/{screeningId}/review")
    public Result<ApplicationAiScreeningReviewVO> reviewScreening(
            @PathVariable("enterpriseId") Long enterpriseId,
            @PathVariable("applicationId") Long applicationId,
            @PathVariable("screeningId") Long screeningId,
            @RequestBody @Valid ApplicationAiReviewReq req) {
        return Result.success(applicationAiScreeningService.review(
                enterpriseId, applicationId, screeningId, req));
    }
}
