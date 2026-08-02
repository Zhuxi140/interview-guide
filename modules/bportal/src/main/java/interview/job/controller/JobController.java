package interview.job.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequirePermission;
import interview.common.annonate.RequireActiveEnterprise;
import interview.job.model.req.JobCreateReq;
import interview.job.model.req.JobListQuery;
import interview.job.model.req.JobStatusReq;
import interview.job.model.req.JobUpdateReq;
import interview.job.model.vo.*;
import interview.job.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author zhuxi
 */
@RestController
@RequireActiveEnterprise
@RequestMapping(ApiVersion.V1 + "/enterprises/{enterpriseId}/jobs")
@Tag(name = "岗位管理")
@RequiredArgsConstructor
@Validated
public class JobController {

    private final JobService jobService;

    @RequirePermission(permissions = Perm.Job.CREATE, scope = PermissionScope.ENTERPRISE)
    @MaxRiskLevel(RiskLevel.MID_RISK)
    @Operation(summary = "发布岗位")
    @PostMapping
    public Result<JobCreateVO> createJob(@PathVariable("enterpriseId") Long enterpriseId,
                                         @RequestBody @Valid JobCreateReq req) {
        JobCreateVO vo = jobService.createJob(enterpriseId, req);
        return Result.success(vo);
    }

    @RequirePermission(permissions = Perm.Job.LIST, scope = PermissionScope.ENTERPRISE)
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @Operation(summary = "查询企业岗位列表（分页）")
    @GetMapping
    public Result<IPage<JobListItemVO>> listJobs(@PathVariable("enterpriseId") Long enterpriseId,
                                                 @Valid JobListQuery query) {
        IPage<JobListItemVO> page = jobService.pageJobs(enterpriseId, query);
        return Result.success(page);
    }

    @RequirePermission(permissions = Perm.Job.DETAIL, scope = PermissionScope.ENTERPRISE)
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @Operation(summary = "查询岗位详情")
    @GetMapping("/{jobId}")
    public Result<JobDetailVO> getJobDetail(@PathVariable("enterpriseId") Long enterpriseId,
                                            @PathVariable("jobId") Long jobId) {
        JobDetailVO vo = jobService.getJobDetail(enterpriseId, jobId);
        return Result.success(vo);
    }

    @RequirePermission(permissions = Perm.Job.UPDATE, scope = PermissionScope.ENTERPRISE)
    @MaxRiskLevel(RiskLevel.MID_RISK)
    @Operation(summary = "编辑岗位")
    @PatchMapping("/{jobId}")
    public Result<JobUpdateVO> updateJob(@PathVariable("enterpriseId") Long enterpriseId,
                                         @PathVariable("jobId") Long jobId,
                                         @RequestBody @Valid JobUpdateReq req) {
        return Result.success(jobService.updateJob(enterpriseId, jobId, req));
    }

    @RequirePermission(permissions = Perm.Job.TOGGLE_STATUS, scope = PermissionScope.ENTERPRISE)
    @MaxRiskLevel(RiskLevel.MID_RISK)
    @Operation(summary = "开关岗位（开放/关闭）")
    @PatchMapping("/{jobId}/status")
    public Result<JobStatusUpdateVO> updateJobStatus(@PathVariable("enterpriseId") Long enterpriseId,
                                                      @PathVariable("jobId") Long jobId,
                                                      @RequestBody @Valid JobStatusReq req) {
        return Result.success(jobService.updateJobStatus(enterpriseId, jobId, req));
    }

    @RequirePermission(permissions = Perm.Job.DELETE, scope = PermissionScope.ENTERPRISE)
    @MaxRiskLevel(RiskLevel.MID_RISK)
    @Operation(summary = "删除岗位（逻辑删除）")
    @DeleteMapping("/{jobId}")
    public Result<Void> deleteJob(@PathVariable("enterpriseId") Long enterpriseId,
                                  @PathVariable("jobId") Long jobId,
                                  @RequestHeader("If-Match")
                                  @Min(value = 0, message = "版本号不能小于 0")
                                  Integer expectedVersion) {
        jobService.deleteJob(enterpriseId, jobId, expectedVersion);
        return Result.success();
    }
}
