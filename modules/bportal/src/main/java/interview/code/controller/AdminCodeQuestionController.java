package interview.code.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.code.model.req.AdminCodeQuestionSearchReq;
import interview.code.model.req.CodeQuestionCreateReq;
import interview.code.model.req.CodeQuestionUpdateReq;
import interview.code.model.vo.CodeQuestionCreateVO;
import interview.code.model.vo.CodeQuestionDetailVO;
import interview.code.model.vo.CodeQuestionListItemVO;
import interview.code.model.vo.CodeQuestionUpdateVO;
import interview.code.service.CodeQuestionService;
import interview.common.annonate.MaxRiskLevel;
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
 * 平台管理端编程题库接口。
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/admin/code-questions")
@Tag(name = "编程题库管理（平台）")
@RequiredArgsConstructor
@Validated
public class AdminCodeQuestionController {

    private final CodeQuestionService codeQuestionService;

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.AdminCodeQuestion.CREATE, scope = PermissionScope.PLATFORM)
    @Operation(summary = "创建平台全局编程题")
    @PostMapping
    public Result<CodeQuestionCreateVO> createQuestion(@RequestBody @Valid CodeQuestionCreateReq req) {
        return Result.success(codeQuestionService.createPlatformQuestion(req));
    }

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.AdminCodeQuestion.LIST, scope = PermissionScope.PLATFORM)
    @Operation(summary = "分页查询平台全局编程题库")
    @GetMapping
    public Result<IPage<CodeQuestionListItemVO>> pageQuestions(@Valid AdminCodeQuestionSearchReq req) {
        return Result.success(codeQuestionService.pagePlatformQuestions(req));
    }

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.AdminCodeQuestion.DETAIL, scope = PermissionScope.PLATFORM)
    @Operation(summary = "查询平台全局编程题详情（含测试用例）")
    @GetMapping("/{questionId}")
    public Result<CodeQuestionDetailVO> getQuestion(@PathVariable Long questionId) {
        return Result.success(codeQuestionService.getPlatformQuestionDetail(questionId));
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.AdminCodeQuestion.UPDATE, scope = PermissionScope.PLATFORM)
    @Operation(summary = "部分更新平台全局编程题")
    @PatchMapping("/{questionId}")
    public Result<CodeQuestionUpdateVO> updateQuestion(
            @PathVariable Long questionId,
            @RequestBody @Valid CodeQuestionUpdateReq req) {
        return Result.success(codeQuestionService.updatePlatformQuestion(questionId, req));
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.AdminCodeQuestion.DELETE, scope = PermissionScope.PLATFORM)
    @Operation(summary = "删除平台全局编程题（逻辑删除）")
    @DeleteMapping("/{questionId}")
    public Result<Void> deleteQuestion(
            @PathVariable Long questionId,
            @RequestHeader("If-Match")
            @Parameter(name = "If-Match", description = "题目当前版本号，用于乐观锁删除校验",
                    required = true, example = "0")
            @Min(value = 0, message = "版本号不能小于 0")
            Integer expectedVersion) {
        codeQuestionService.deletePlatformQuestion(questionId, expectedVersion);
        return Result.success();
    }
}
