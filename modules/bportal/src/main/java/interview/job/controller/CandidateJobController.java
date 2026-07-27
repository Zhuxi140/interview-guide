package interview.job.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.annonate.MaxRiskLevel;
import interview.common.constant.ApiVersion;
import interview.common.constant.Result;
import interview.common.enums.RiskLevel;
import interview.job.model.req.CandidateJobSearchReq;
import interview.job.model.vo.CandidateJobDetailVO;
import interview.job.model.vo.CandidateJobListItemVO;
import interview.job.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zhuxi
 * @apiNote C 端岗位发现接口
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/jobs")
@Tag(name = "C 端岗位发现")
@RequiredArgsConstructor
public class CandidateJobController {

    private final JobService jobService;

    @MaxRiskLevel(RiskLevel.HIGH_RISK)
    @Operation(summary = "查询可投递岗位")
    @GetMapping
    public Result<IPage<CandidateJobListItemVO>> listCandidateJobs(
            @Valid @ParameterObject CandidateJobSearchReq req) {
        IPage<CandidateJobListItemVO> page = jobService.pageCandidateJobs(req);
        return Result.success(page);
    }

    @MaxRiskLevel(RiskLevel.HIGH_RISK)
    @Operation(summary = "查询可投递岗位详情")
    @GetMapping("/{jobId}")
    public Result<CandidateJobDetailVO> getCandidateJobDetail(
            @PathVariable("jobId") Long jobId) {
        CandidateJobDetailVO vo = jobService.getCandidateJobDetail(jobId);
        return Result.success(vo);
    }
}
