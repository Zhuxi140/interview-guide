package interview.matching.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequirePermission;
import interview.matching.model.enums.JobApplicationStatus;
import interview.matching.model.req.*;
import interview.matching.model.vo.JobApplicationListItemVO;
import interview.matching.model.vo.JobApplicationStatusVO;
import interview.matching.model.vo.JobApplicationSubmitVO;
import interview.matching.model.vo.JobApplicationVO;
import interview.matching.model.vo.MyApplicationListItemVO;
import interview.matching.model.vo.CandidateJobMatchAnalysisVO;
import interview.matching.model.vo.CandidateJobMatchTriggerVO;
import interview.matching.service.JobApplicationsService;
import interview.matching.service.CandidateJobMatchAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author zhuxi
 */
@RestController
@RequestMapping(ApiVersion.V1)
@Tag(name = "投递与初筛")
@RequiredArgsConstructor
@Validated
public class JobApplicationsController {

    private final JobApplicationsService jobApplicationsService;
    private final CandidateJobMatchAnalysisService candidateJobMatchAnalysisService;

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @Operation(summary = "候选人投递简历")
    @PostMapping("/jobs/{jobId}/applications")
    public Result<JobApplicationSubmitVO> submitApplication(@PathVariable("jobId") Long jobId,
                                                             @RequestBody @Valid JobApplicationSubmitReq req,
                                                             @RequestHeader("Idempotency-Key")
                                                             @NotBlank @Size(max = 128)
                                                             String idempotencyKey) {
        JobApplicationSubmitVO vo = jobApplicationsService.submitApplication(
                jobId, req, idempotencyKey);
        return Result.success(vo);
    }

    @RequirePermission(permissions = Perm.Application.LIST, scope = PermissionScope.ENTERPRISE)
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @Operation(summary = "查询岗位投递列表（分页，HR 端）")
    @GetMapping("/enterprises/{enterpriseId}/jobs/{jobId}/applications")
    public Result<IPage<JobApplicationListItemVO>> listApplications(
            @PathVariable("enterpriseId") Long enterpriseId,
            @PathVariable("jobId") Long jobId,
            @Valid @ParameterObject JobApplicationPageReq req) {
        IPage<JobApplicationListItemVO> result =
                jobApplicationsService.pageApplications(enterpriseId, jobId, req);
        return Result.success(result);
    }

    @RequirePermission(permissions = Perm.Application.DETAIL, scope = PermissionScope.ENTERPRISE)
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @Operation(summary = "查询投递详情（含最新 HR AI 初筛建议）")
    @GetMapping("/enterprises/{enterpriseId}/applications/{applicationId}")
    public Result<JobApplicationVO> getApplicationDetail(
            @PathVariable("enterpriseId") Long enterpriseId,
            @PathVariable("applicationId") Long applicationId) {
        JobApplicationVO vo = jobApplicationsService.getApplicationDetail(enterpriseId, applicationId);
        return Result.success(vo);
    }

    @RequirePermission(permissions = Perm.Application.UPDATE, scope = PermissionScope.ENTERPRISE)
    @MaxRiskLevel(RiskLevel.MID_RISK)
    @Operation(summary = "HR 更新投递状态")
    @PatchMapping("/enterprises/{enterpriseId}/applications/{applicationId}/status")
    public Result<JobApplicationStatusVO> updateApplicationStatus(
            @PathVariable("enterpriseId") Long enterpriseId,
            @PathVariable("applicationId") Long applicationId,
            @RequestBody @Valid JobApplicationStatusReq req) {
        return Result.success(jobApplicationsService.updateApplicationStatus(
                enterpriseId, applicationId, req));
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @Operation(summary = "C 端查询我的投递记录（分页）")
    @GetMapping("/candidate/applications")
    public Result<IPage<MyApplicationListItemVO>> listMyApplications(
            @Valid @ParameterObject JobApplicationPageReq req) {
        IPage<MyApplicationListItemVO> result = jobApplicationsService.pageMyApplications(req);
        return Result.success(result);
    }

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @Operation(summary = "候选人撤回投递")
    @PostMapping("/candidate/applications/{applicationId}/withdraw")
    public Result<JobApplicationStatusVO> withdrawApplication(
            @PathVariable("applicationId") Long applicationId,
            @RequestBody @Valid JobApplicationWithdrawReq req) {
        return Result.success(
                jobApplicationsService.withdrawApplication(applicationId, req));
    }

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @Operation(summary = "候选人发起岗位适配预测")
    @PostMapping("/candidate/applications/{applicationId}/match-analyses")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Result<CandidateJobMatchTriggerVO> createMatchAnalysis(
            @PathVariable("applicationId") Long applicationId,
            @RequestHeader("Idempotency-Key")
            @NotBlank @Size(max = 128) String idempotencyKey) {
        return Result.success(candidateJobMatchAnalysisService.accept(
                applicationId, idempotencyKey));
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @Operation(summary = "候选人查询最新岗位适配预测")
    @GetMapping("/candidate/applications/{applicationId}/match-analyses/latest")
    public Result<CandidateJobMatchAnalysisVO> getLatestMatchAnalysis(
            @PathVariable("applicationId") Long applicationId) {
        return Result.success(candidateJobMatchAnalysisService.getLatest(applicationId));
    }
}
