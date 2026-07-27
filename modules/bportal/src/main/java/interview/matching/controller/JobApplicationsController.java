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
import interview.matching.service.JobApplicationsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
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
    @Operation(summary = "查询投递详情（含 AI 人岗匹配分）")
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
}
