package interview.job.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.constant.ApiVersion;
import interview.common.constant.Result;
import interview.job.model.req.JobCreateReq;
import interview.job.model.req.JobListQuery;
import interview.job.model.req.JobStatusReq;
import interview.job.model.req.JobUpdateReq;
import interview.job.model.vo.*;
import interview.job.service.JobService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @author zhuxi
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/enterprises/{enterpriseId}/jobs")
@Tag(name = "岗位管理")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @ApiResponse(description = "发布岗位")
    @PostMapping
    public Result<JobCreateVO> createJob(@PathVariable("enterpriseId") Long enterpriseId,
                                         @RequestBody @Valid JobCreateReq req) {
        JobCreateVO vo = jobService.createJob(enterpriseId, req);
        return Result.success(vo);
    }

    @ApiResponse(description = "查询企业岗位列表（分页）")
    @GetMapping
    public Result<IPage<JobListItemVO>> listJobs(@PathVariable("enterpriseId") Long enterpriseId,
                                                 @Valid JobListQuery query) {
        IPage<JobListItemVO> page = jobService.pageJobs(enterpriseId, query);
        return Result.success(page);
    }

    @ApiResponse(description = "查询岗位详情")
    @GetMapping("/{jobId}")
    public Result<JobDetailVO> getJobDetail(@PathVariable("enterpriseId") Long enterpriseId,
                                            @PathVariable("jobId") Long jobId) {
        JobDetailVO vo = jobService.getJobDetail(enterpriseId, jobId);
        return Result.success(vo);
    }

    @ApiResponse(description = "编辑岗位")
    @PutMapping("/{jobId}")
    public Result<JobUpdateVO> updateJob(@PathVariable("enterpriseId") Long enterpriseId,
                                         @PathVariable("jobId") Long jobId,
                                         @RequestBody @Valid JobUpdateReq req) {
        JobUpdateVO vo = jobService.updateJob(enterpriseId, jobId, req);
        return Result.success(vo);
    }

    @ApiResponse(description = "开关岗位（开放/关闭）")
    @PatchMapping("/{jobId}/status")
    public Result<JobStatusVO> updateJobStatus(@PathVariable("enterpriseId") Long enterpriseId,
                                               @PathVariable("jobId") Long jobId,
                                               @RequestBody @Valid JobStatusReq req) {
        JobStatusVO vo = jobService.updateJobStatus(enterpriseId, jobId, req);
        return Result.success(vo);
    }

    @ApiResponse(description = "删除岗位（逻辑删除）")
    @DeleteMapping("/{jobId}")
    public Result<JobDeleteVO> deleteJob(@PathVariable("enterpriseId") Long enterpriseId,
                                         @PathVariable("jobId") Long jobId) {
        JobDeleteVO vo = jobService.deleteJob(enterpriseId, jobId);
        return Result.success(vo);
    }
}
