package interview.ai.providerconfig.controller;

import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.ai.providerconfig.model.req.LlmProviderCreateReq;
import interview.ai.providerconfig.model.req.LlmProviderQueryReq;
import interview.ai.providerconfig.model.req.LlmProviderStatusReq;
import interview.ai.providerconfig.model.req.LlmProviderUpdateReq;
import interview.ai.providerconfig.model.vo.LlmProviderDeleteVO;
import interview.ai.providerconfig.model.vo.LlmProviderPageVO;
import interview.ai.providerconfig.model.vo.LlmProviderStatusVO;
import interview.ai.providerconfig.model.vo.LlmProviderTestConnectionVO;
import interview.ai.providerconfig.model.vo.LlmProviderVO;
import interview.ai.providerconfig.service.LlmProviderConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiVersion.V1 + "/admin/llm/providers")
@Tag(name = "大模型提供商管理（平台域）")
@RequiredArgsConstructor
@Validated
public class LlmProviderController {

    private final LlmProviderConfigService llmProviderConfigService;

    @Operation(summary = "创建大模型提供商配置")
    @RequirePermission(
            permissions = Perm.AdminLlm.PROVIDER_CREATE,
            scope = PermissionScope.PLATFORM
    )
    @PostMapping
    public Result<LlmProviderVO> createProvider(
            @Valid @RequestBody LlmProviderCreateReq req) {
        return Result.success(llmProviderConfigService.createProvider(req));
    }

    @Operation(summary = "分页查询大模型提供商配置")
    @RequirePermission(
            permissions = Perm.AdminLlm.PROVIDER_LIST,
            scope = PermissionScope.PLATFORM
    )
    @GetMapping
    public Result<LlmProviderPageVO> pageProviders(
            @Valid LlmProviderQueryReq req) {
        return Result.success(llmProviderConfigService.pageProviders(req));
    }

    @Operation(summary = "查询大模型提供商配置详情")
    @RequirePermission(
            permissions = Perm.AdminLlm.PROVIDER_DETAIL,
            scope = PermissionScope.PLATFORM
    )
    @GetMapping("/{providerId}")
    public Result<LlmProviderVO> getProvider(@PathVariable String providerId) {
        return Result.success(llmProviderConfigService.getProvider(providerId));
    }

    @Operation(summary = "更新大模型提供商配置（CAS 乐观锁）")
    @RequirePermission(
            permissions = Perm.AdminLlm.PROVIDER_UPDATE,
            scope = PermissionScope.PLATFORM
    )
    @PatchMapping("/{providerId}")
    public Result<LlmProviderVO> updateProvider(
            @PathVariable String providerId,
            @Valid @RequestBody LlmProviderUpdateReq req) {
        return Result.success(llmProviderConfigService.updateProvider(providerId, req));
    }

    @Operation(summary = "启停大模型提供商（CAS 乐观锁）")
    @RequirePermission(
            permissions = Perm.AdminLlm.PROVIDER_STATUS,
            scope = PermissionScope.PLATFORM
    )
    @PatchMapping("/{providerId}/status")
    public Result<LlmProviderStatusVO> updateProviderStatus(
            @PathVariable String providerId,
            @Valid @RequestBody LlmProviderStatusReq req) {
        return Result.success(llmProviderConfigService.updateProviderStatus(providerId, req));
    }

    @Operation(summary = "逻辑删除大模型提供商配置（CAS 乐观锁）")
    @RequirePermission(
            permissions = Perm.AdminLlm.PROVIDER_DELETE,
            scope = PermissionScope.PLATFORM
    )
    @DeleteMapping("/{providerId}")
    public Result<LlmProviderDeleteVO> deleteProvider(
            @PathVariable String providerId,
            @RequestParam Integer expectedVersion) {
        return Result.success(llmProviderConfigService.deleteProvider(providerId, expectedVersion));
    }

    @Operation(summary = "测试大模型提供商连接")
    @RequirePermission(
            permissions = Perm.AdminLlm.PROVIDER_TEST,
            scope = PermissionScope.PLATFORM
    )
    @PostMapping("/{providerId}/test-connection")
    public Result<LlmProviderTestConnectionVO> testConnection(
            @PathVariable String providerId) {
        return Result.success(llmProviderConfigService.testConnection(providerId));
    }
}
