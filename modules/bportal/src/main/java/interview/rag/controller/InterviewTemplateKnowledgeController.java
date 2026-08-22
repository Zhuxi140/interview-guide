package interview.rag.controller;

import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequireActiveEnterprise;
import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import interview.rag.model.req.TemplateKnowledgeBindReq;
import interview.rag.model.vo.TemplateKnowledgeBindVO;
import interview.rag.service.RagSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业端面试模板知识库绑定接口。
 */
@RestController
@RequireActiveEnterprise
@RequestMapping(ApiVersion.V1 + "/enterprises/{enterpriseId}/interview-templates/{templateId}/knowledge-bases")
@Tag(name = "面试模板知识库绑定（企业）")
@RequiredArgsConstructor
@Validated
public class InterviewTemplateKnowledgeController {

    private final RagSessionService ragSessionService;

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.Rag.KNOWLEDGE_BIND, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "完整替换面试模板允许检索的知识库集合")
    @PutMapping
    public Result<TemplateKnowledgeBindVO> bindKnowledgeBases(
            @PathVariable("enterpriseId") Long enterpriseId,
            @PathVariable("templateId") Long templateId,
            @RequestBody @Valid TemplateKnowledgeBindReq req) {
        return Result.success(
                ragSessionService.bindTemplateKnowledgeBases(enterpriseId, templateId, req));
    }
}
