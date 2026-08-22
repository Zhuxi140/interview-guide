package interview.knowledge.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequireActiveEnterprise;
import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import interview.knowledge.model.req.EnterpriseKnowledgeBaseSearchReq;
import interview.knowledge.model.vo.KnowledgeBaseDetailVO;
import interview.knowledge.model.vo.KnowledgeBaseListItemVO;
import interview.knowledge.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业端知识库文档接口（企业私有文档 + 可用全局文档）。
 */
@RestController
@RequireActiveEnterprise
@RequestMapping(ApiVersion.V1 + "/enterprises/{enterpriseId}/knowledge-bases")
@Tag(name = "知识库文档管理（企业）")
@RequiredArgsConstructor
@Validated
public class EnterpriseKnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.KnowledgeBase.LIST, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "分页查询企业私有文档与可用全局文档")
    @GetMapping
    public Result<IPage<KnowledgeBaseListItemVO>> pageKnowledgeBases(
            @PathVariable("enterpriseId") Long enterpriseId,
            @Valid EnterpriseKnowledgeBaseSearchReq req) {
        return Result.success(
                knowledgeBaseService.pageEnterpriseKnowledgeBases(enterpriseId, req));
    }

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.KnowledgeBase.DETAIL, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "查询企业有权访问的文档详情（含切片数量统计）")
    @GetMapping("/{kbId}")
    public Result<KnowledgeBaseDetailVO> getKnowledgeBase(
            @PathVariable("enterpriseId") Long enterpriseId,
            @PathVariable("kbId") Long kbId) {
        return Result.success(
                knowledgeBaseService.getEnterpriseKnowledgeBaseDetail(enterpriseId, kbId));
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.KnowledgeBase.DELETE, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "删除企业私有文档（逻辑删除）")
    @DeleteMapping("/{kbId}")
    public Result<Void> deleteKnowledgeBase(
            @PathVariable("enterpriseId") Long enterpriseId,
            @PathVariable("kbId") Long kbId,
            @RequestHeader("If-Match")
            @Parameter(name = "If-Match", description = "文档当前版本号，用于乐观锁删除校验",
                    required = true, example = "0")
            @Min(value = 0, message = "版本号不能小于 0")
            Integer expectedVersion) {
        knowledgeBaseService.deleteEnterpriseKnowledgeBase(enterpriseId, kbId, expectedVersion);
        return Result.success();
    }
}
