package interview.candidate.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.candidate.model.req.ResumeImportItemPageReq;
import interview.candidate.model.vo.ResumeImportBatchVO;
import interview.candidate.model.vo.ResumeImportItemVO;
import interview.candidate.service.ResumeImportQueryService;
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
@RequestMapping(ApiVersion.V1 + "/enterprises/{enterpriseId}/resume-import-batches")
@Tag(name = "企业简历批量导入查询（B端）")
@RequiredArgsConstructor
public class ResumeImportQueryController {

    private final ResumeImportQueryService resumeImportQueryService;

    @Operation(summary = "查询简历导入批次进度")
    @RequirePermission(permissions = Perm.ResumeImport.DETAIL, scope = PermissionScope.ENTERPRISE)
    @RequireActiveEnterprise
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @GetMapping("/{batchId}")
    public Result<ResumeImportBatchVO> getBatch(
            @PathVariable Long enterpriseId, @PathVariable Long batchId) {
        return Result.success(resumeImportQueryService.getBatch(enterpriseId, batchId));
    }

    @Operation(summary = "分页查询简历导入逐文件结果")
    @RequirePermission(permissions = Perm.ResumeImport.ITEMS, scope = PermissionScope.ENTERPRISE)
    @RequireActiveEnterprise
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @GetMapping("/{batchId}/items")
    public Result<IPage<ResumeImportItemVO>> pageItems(
            @PathVariable Long enterpriseId,
            @PathVariable Long batchId,
            @Valid @ParameterObject ResumeImportItemPageReq req) {
        return Result.success(resumeImportQueryService.pageItems(
                enterpriseId, batchId, req));
    }
}
