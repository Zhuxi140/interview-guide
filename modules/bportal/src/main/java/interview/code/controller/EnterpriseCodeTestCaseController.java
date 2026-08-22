package interview.code.controller;

import interview.code.model.req.CodeTestCaseCreateReq;
import interview.code.model.req.CodeTestCaseUpdateReq;
import interview.code.model.vo.CodeTestCaseCreateVO;
import interview.code.model.vo.CodeTestCaseUpdateVO;
import interview.code.service.CodeTestCaseService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业端测试用例接口（企业私有题）。
 */
@RestController
@RequireActiveEnterprise
@RequestMapping(ApiVersion.V1
        + "/enterprises/{enterpriseId}/code-questions/{questionId}/test-cases")
@Tag(name = "测试用例管理（企业）")
@RequiredArgsConstructor
@Validated
public class EnterpriseCodeTestCaseController {

    private final CodeTestCaseService codeTestCaseService;

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.CodeTestCase.CREATE, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "为企业私有题添加测试用例")
    @PostMapping
    public Result<CodeTestCaseCreateVO> createTestCase(
            @PathVariable("enterpriseId") Long enterpriseId,
            @PathVariable Long questionId,
            @RequestBody @Valid CodeTestCaseCreateReq req) {
        return Result.success(
                codeTestCaseService.createEnterpriseTestCase(enterpriseId, questionId, req));
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.CodeTestCase.UPDATE, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "更新企业私有题测试用例")
    @PatchMapping("/{caseId}")
    public Result<CodeTestCaseUpdateVO> updateTestCase(
            @PathVariable("enterpriseId") Long enterpriseId,
            @PathVariable Long questionId,
            @PathVariable Long caseId,
            @RequestBody @Valid CodeTestCaseUpdateReq req) {
        return Result.success(codeTestCaseService.updateEnterpriseTestCase(
                enterpriseId, questionId, caseId, req));
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.CodeTestCase.DELETE, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "删除企业私有题测试用例（逻辑删除）")
    @DeleteMapping("/{caseId}")
    public Result<Void> deleteTestCase(
            @PathVariable("enterpriseId") Long enterpriseId,
            @PathVariable Long questionId,
            @PathVariable Long caseId,
            @RequestHeader("If-Match")
            @Parameter(name = "If-Match", description = "用例当前版本号，用于乐观锁删除校验",
                    required = true, example = "0")
            @Min(value = 0, message = "版本号不能小于 0")
            Integer expectedVersion) {
        codeTestCaseService.deleteEnterpriseTestCase(
                enterpriseId, questionId, caseId, expectedVersion);
        return Result.success();
    }
}
