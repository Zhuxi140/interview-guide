package interview.compliance.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import interview.compliance.model.req.SensitiveWordCreateReq;
import interview.compliance.model.req.SensitiveWordSearchReq;
import interview.compliance.model.req.SensitiveWordUpdateReq;
import interview.compliance.model.vo.SensitiveWordCreateVO;
import interview.compliance.model.vo.SensitiveWordListItemVO;
import interview.compliance.model.vo.SensitiveWordUpdateVO;
import interview.compliance.service.SensitiveWordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zhuxi
 * @apiNote 平台端敏感词库管理
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/admin/sensitive-words")
@Tag(name = "敏感词库管理（平台）")
@RequiredArgsConstructor
@Validated
public class SensitiveWordsController {

    private final SensitiveWordService sensitiveWordService;

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.SensitiveWords.LIST, scope = PermissionScope.PLATFORM)
    @Operation(summary = "分页查询敏感词库（按类别筛选）")
    @GetMapping
    public Result<IPage<SensitiveWordListItemVO>> pageSensitiveWords(
            @Valid @ParameterObject SensitiveWordSearchReq req) {
        return Result.success(sensitiveWordService.pageSensitiveWords(req));
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.SensitiveWords.CREATE, scope = PermissionScope.PLATFORM)
    @Operation(summary = "添加敏感词")
    @PostMapping
    public Result<SensitiveWordCreateVO> createSensitiveWord(
            @RequestBody @Valid SensitiveWordCreateReq req) {
        return Result.success(sensitiveWordService.createSensitiveWord(req));
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.SensitiveWords.UPDATE, scope = PermissionScope.PLATFORM)
    @Operation(summary = "编辑敏感词（半量更新 + 乐观锁）")
    @PatchMapping("/{id}")
    public Result<SensitiveWordUpdateVO> updateSensitiveWord(
            @PathVariable Long id,
            @RequestBody @Valid SensitiveWordUpdateReq req) {
        return Result.success(sensitiveWordService.updateSensitiveWord(id, req));
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.SensitiveWords.DELETE, scope = PermissionScope.PLATFORM)
    @Operation(summary = "删除敏感词（逻辑删除 + If-Match 版本校验）")
    @DeleteMapping("/{id}")
    public Result<Void> deleteSensitiveWord(
            @PathVariable Long id,
            @RequestHeader("If-Match")
            @Parameter(name = "If-Match", description = "敏感词当前版本号，用于乐观锁删除校验",
                    required = true, example = "0")
            @Min(value = 0, message = "版本号不能小于 0")
            Integer expectedVersion) {
        sensitiveWordService.deleteSensitiveWord(id, expectedVersion);
        return Result.success();
    }
}
