package interview.rag.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequireActiveEnterprise;
import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import interview.rag.model.req.RagMessageCursorReq;
import interview.rag.model.req.RagSessionCreateReq;
import interview.rag.model.req.RagSessionKnowledgeBindReq;
import interview.rag.model.req.RagSessionSearchReq;
import interview.rag.model.vo.RagMessagePageVO;
import interview.rag.model.vo.RagSessionCreateVO;
import interview.rag.model.vo.RagSessionKnowledgeBindVO;
import interview.rag.model.vo.RagSessionListItemVO;
import interview.rag.service.RagSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业端 RAG 对话接口（会话创建/查询、知识库绑定、消息游标分页）。
 */
@RestController
@RequireActiveEnterprise
@RequestMapping(ApiVersion.V1 + "/enterprises/{enterpriseId}/rag/sessions")
@Tag(name = "RAG 对话（企业）")
@RequiredArgsConstructor
@Validated
public class RagSessionController {

    private final RagSessionService ragSessionService;

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.Rag.SESSION_CREATE, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "创建 RAG 对话会话")
    @PostMapping
    public Result<RagSessionCreateVO> createSession(
            @PathVariable("enterpriseId") Long enterpriseId,
            @RequestBody @Valid RagSessionCreateReq req) {
        return Result.success(ragSessionService.createSession(enterpriseId, req));
    }

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.Rag.SESSION_LIST, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "分页查询本人在该企业下的 RAG 会话")
    @GetMapping
    public Result<IPage<RagSessionListItemVO>> pageSessions(
            @PathVariable("enterpriseId") Long enterpriseId,
            @Valid RagSessionSearchReq req) {
        return Result.success(ragSessionService.pageMySessions(enterpriseId, req));
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.Rag.KNOWLEDGE_BIND, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "完整替换会话绑定的知识库集合")
    @PutMapping("/{sessionId}/knowledge-bases")
    public Result<RagSessionKnowledgeBindVO> bindKnowledgeBases(
            @PathVariable("enterpriseId") Long enterpriseId,
            @PathVariable("sessionId") Long sessionId,
            @RequestBody @Valid RagSessionKnowledgeBindReq req) {
        return Result.success(
                ragSessionService.bindSessionKnowledgeBases(enterpriseId, sessionId, req));
    }

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.Rag.MESSAGE_LIST, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "游标分页查询会话消息（按时间正序）")
    @GetMapping("/{sessionId}/messages")
    public Result<RagMessagePageVO> listMessages(
            @PathVariable("enterpriseId") Long enterpriseId,
            @PathVariable("sessionId") Long sessionId,
            @Valid RagMessageCursorReq req) {
        return Result.success(ragSessionService.listMessages(enterpriseId, sessionId, req));
    }
}
