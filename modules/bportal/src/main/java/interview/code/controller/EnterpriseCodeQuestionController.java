package interview.code.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.code.model.req.CodeQuestionCreateReq;
import interview.code.model.req.CodeQuestionUpdateReq;
import interview.code.model.req.EnterpriseCodeQuestionSearchReq;
import interview.code.model.vo.CodeQuestionCreateVO;
import interview.code.model.vo.CodeQuestionDetailVO;
import interview.code.model.vo.CodeQuestionListItemVO;
import interview.code.model.vo.CodeQuestionUpdateVO;
import interview.code.service.CodeQuestionService;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequireActiveEnterprise;
import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业端编程题库接口（企业私有题 + 可用全局题）。
 */
@RestController
@RequireActiveEnterprise
@RequestMapping(ApiVersion.V1 + "/enterprises/{enterpriseId}/code-questions")
@Tag(name = "编程题库管理（企业）")
@RequiredArgsConstructor
@Validated
public class EnterpriseCodeQuestionController {

    private final CodeQuestionService codeQuestionService;

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.AdminCodeQuestion.CREATE, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "创建企业私有编程题")
    @PostMapping
    public Result<CodeQuestionCreateVO> createQuestion(
            @PathVariable("enterpriseId") Long enterpriseId,
            @RequestBody @Valid CodeQuestionCreateReq req) {
        return Result.success(codeQuestionService.createEnterpriseQuestion(enterpriseId, req));
    }

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.AdminCodeQuestion.LIST, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "分页查询企业私有题和可用全局题")
    @GetMapping
    public Result<IPage<CodeQuestionListItemVO>> pageQuestions(
            @PathVariable("enterpriseId") Long enterpriseId,
            @Valid EnterpriseCodeQuestionSearchReq req) {
        return Result.success(codeQuestionService.pageEnterpriseQuestions(enterpriseId, req));
    }

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.AdminCodeQuestion.DETAIL, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "查询企业有权管理的题目详情（含测试用例）")
    @GetMapping("/{questionId}")
    public Result<CodeQuestionDetailVO> getQuestion(
            @PathVariable("enterpriseId") Long enterpriseId,
            @PathVariable Long questionId) {
        return Result.success(
                codeQuestionService.getEnterpriseQuestionDetail(enterpriseId, questionId));
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.AdminCodeQuestion.UPDATE, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "更新企业私有编程题")
    @PatchMapping("/{questionId}")
    public Result<CodeQuestionUpdateVO> updateQuestion(
            @PathVariable("enterpriseId") Long enterpriseId,
            @PathVariable Long questionId,
            @RequestBody @Valid CodeQuestionUpdateReq req) {
        return Result.success(
                codeQuestionService.updateEnterpriseQuestion(enterpriseId, questionId, req));
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.AdminCodeQuestion.DELETE, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "删除企业私有编程题（逻辑删除）")
    @DeleteMapping("/{questionId}")
    public Result<Void> deleteQuestion(
            @PathVariable("enterpriseId") Long enterpriseId,
            @PathVariable Long questionId,
            @RequestHeader("If-Match")
            @Parameter(name = "If-Match", description = "题目当前版本号，用于乐观锁删除校验",
                    required = true, example = "0")
            @Min(value = 0, message = "版本号不能小于 0")
            Integer expectedVersion) {
        codeQuestionService.deleteEnterpriseQuestion(enterpriseId, questionId, expectedVersion);
        return Result.success();
    }
}
