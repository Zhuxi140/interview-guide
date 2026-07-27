package interview.ai.providerconfig.controller;

import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.ai.providerconfig.model.req.LlmSettingUpdateReq;
import interview.ai.providerconfig.model.vo.LlmSettingVO;
import interview.ai.providerconfig.service.LlmGlobalSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiVersion.V1 + "/admin/llm/settings")
@Tag(name = "LLM 全局设置管理（平台域）")
@RequiredArgsConstructor
@Validated
public class LlmSettingController {

    private final LlmGlobalSettingService llmGlobalSettingService;

    @Operation(summary = "查询 LLM 全局设置")
    @RequirePermission(
            permissions = Perm.AdminLlm.SETTING_DETAIL,
            scope = PermissionScope.PLATFORM
    )
    @GetMapping
    public Result<LlmSettingVO> getSetting() {
        return Result.success(llmGlobalSettingService.getSetting());
    }

    @Operation(summary = "更新 LLM 全局设置（CAS 乐观锁）")
    @RequirePermission(
            permissions = Perm.AdminLlm.SETTING_UPDATE,
            scope = PermissionScope.PLATFORM
    )
    @PutMapping
    public Result<LlmSettingVO> updateSetting(
            @Valid @RequestBody LlmSettingUpdateReq req) {
        return Result.success(llmGlobalSettingService.updateSetting(req));
    }
}
