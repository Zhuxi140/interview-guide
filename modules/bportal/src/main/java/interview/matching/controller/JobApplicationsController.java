package interview.matching.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import interview.framework.annonate.MaxRiskLevel;
import interview.framework.annonate.RequirePermission;
import interview.matching.model.enums.JobApplicationStatus;
import interview.matching.model.req.JobApplicationStatusReq;
import interview.matching.model.req.JobApplicationSubmitReq;
import interview.matching.model.vo.JobApplicationListItemVO;
import interview.matching.model.vo.JobApplicationVO;
import interview.matching.model.vo.MyApplicationListItemVO;
import interview.matching.service.JobApplicationsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @author zhuxi
 */
@RestController
@RequestMapping(ApiVersion.V1)
@Tag(name = "投递与初筛")
@RequiredArgsConstructor
public class JobApplicationsController {

    private final JobApplicationsService jobApplicationsService;

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @Operation(summary = "候选人投递简历")
    @PostMapping("/candidate/jobs/{jobId}/apply")
    public Result<JobApplicationVO> submitApplication(@PathVariable("jobId") Long jobId,
                                                      @RequestBody @Valid JobApplicationSubmitReq req) {
        JobApplicationVO vo = jobApplicationsService.submitApplication(jobId, req);
        return Result.success(vo);
    }

    @RequirePermission(permissions = Perm.Application.LIST, scope = PermissionScope.ENTERPRISE)
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @Operation(summary = "查询岗位投递列表（分页，HR 端）")
    @GetMapping("/enterprises/{enterpriseId}/jobs/{jobId}/applications")
    public Result<IPage<JobApplicationListItemVO>> listApplications(
            @PathVariable("enterpriseId") Long enterpriseId,
            @PathVariable("jobId") Long jobId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) Integer size,
            @RequestParam(required = false) JobApplicationStatus status) {
        IPage<JobApplicationListItemVO> result = jobApplicationsService.pageApplications(enterpriseId, jobId, page, size, status);
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
    public Result<Void> updateApplicationStatus(
            @PathVariable("enterpriseId") Long enterpriseId,
            @PathVariable("applicationId") Long applicationId,
            @RequestBody @Valid JobApplicationStatusReq req) {
        jobApplicationsService.updateApplicationStatus(enterpriseId, applicationId, req);
        return Result.success();
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @Operation(summary = "C 端查询我的投递记录（分页）")
    @GetMapping("/candidate/applications")
    public Result<IPage<MyApplicationListItemVO>> listMyApplications(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) Integer size,
            @RequestParam(required = false) JobApplicationStatus status) {
        IPage<MyApplicationListItemVO> result = jobApplicationsService.pageMyApplications(page, size, status);
        return Result.success(result);
    }
}
