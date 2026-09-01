package interview.candidate.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.candidate.model.req.EnterpriseCandidatePageReq;
import interview.candidate.model.vo.EnterpriseCandidateDetailVO;
import interview.candidate.model.vo.EnterpriseCandidateListItemVO;
import interview.candidate.model.vo.EnterpriseCandidateOverviewVO;
import interview.candidate.service.EnterpriseCandidateService;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiVersion.V1 + "/enterprises/{enterpriseId}/candidates")
@Tag(name = "企业人才池（B端）")
@RequiredArgsConstructor
public class EnterpriseCandidateController {

    private final EnterpriseCandidateService enterpriseCandidateService;

    @Operation(summary = "分页查询企业人才池")
    @RequirePermission(permissions = Perm.EnterpriseCandidate.LIST, scope = PermissionScope.ENTERPRISE)
    @RequireActiveEnterprise
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @GetMapping
    public Result<IPage<EnterpriseCandidateListItemVO>> pageCandidates(
            @PathVariable Long enterpriseId,
            @Valid @ParameterObject EnterpriseCandidatePageReq req) {
        return Result.success(enterpriseCandidateService.pageCandidates(enterpriseId, req));
    }

    @Operation(summary = "查询企业人才池候选人详情")
    @RequirePermission(permissions = Perm.EnterpriseCandidate.DETAIL, scope = PermissionScope.ENTERPRISE)
    @RequireActiveEnterprise
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @GetMapping("/{candidateId}")
    public Result<EnterpriseCandidateDetailVO> getCandidate(
            @PathVariable Long enterpriseId, @PathVariable Long candidateId) {
        return Result.success(enterpriseCandidateService.getCandidate(
                enterpriseId, candidateId));
    }

    @Operation(summary = "查询企业候选人综合看板")
    @RequirePermission(permissions = Perm.EnterpriseCandidate.OVERVIEW, scope = PermissionScope.ENTERPRISE)
    @RequireActiveEnterprise
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @GetMapping("/{candidateId}/overview")
    public Result<EnterpriseCandidateOverviewVO> getOverview(
            @PathVariable Long enterpriseId, @PathVariable Long candidateId) {
        return Result.success(enterpriseCandidateService.getOverview(
                enterpriseId, candidateId));
    }
}
