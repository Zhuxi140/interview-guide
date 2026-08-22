package interview.code.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.code.model.req.EnterpriseCodeSubmissionSearchReq;
import interview.code.model.vo.EnterpriseCodeSubmissionDetailVO;
import interview.code.model.vo.EnterpriseCodeSubmissionListItemVO;
import interview.code.service.CodeSubmissionQueryService;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequireActiveEnterprise;
import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业侧候选人代码提交查询接口（HR/面试官）。
 */
@RestController
@RequireActiveEnterprise
@RequestMapping(ApiVersion.V1 + "/enterprises/{enterpriseId}")
@Tag(name = "候选人代码提交查询（企业）")
@RequiredArgsConstructor
@Validated
public class EnterpriseCodeSubmissionController {

    private final CodeSubmissionQueryService codeSubmissionQueryService;

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.CodeSubmission.RESULTS, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "查询本企业候选人的代码提交记录（分页）")
    @GetMapping("/candidates/{candidateId}/code-submissions")
    public Result<IPage<EnterpriseCodeSubmissionListItemVO>> pageCandidateSubmissions(
            @PathVariable("enterpriseId") Long enterpriseId,
            @PathVariable Long candidateId,
            @Valid EnterpriseCodeSubmissionSearchReq req) {
        return Result.success(codeSubmissionQueryService.pageEnterpriseCandidateSubmissions(
                enterpriseId, candidateId, req));
    }

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.CodeSubmission.RESULTS, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "查询提交详情（执行结果 + AI 审查全文）")
    @GetMapping("/code-submissions/{submissionId}")
    public Result<EnterpriseCodeSubmissionDetailVO> getSubmissionDetail(
            @PathVariable("enterpriseId") Long enterpriseId,
            @PathVariable Long submissionId) {
        return Result.success(
                codeSubmissionQueryService.getEnterpriseSubmissionDetail(
                        enterpriseId, submissionId));
    }
}
