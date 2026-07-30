package interview.ai.config.controller;

import interview.common.enums.AiSceneCode;
import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.ai.config.model.req.LlmSceneQueryReq;
import interview.ai.config.model.req.LlmSceneStatusReq;
import interview.ai.config.model.req.LlmSceneUpdateReq;
import interview.ai.config.model.vo.LlmScenePageVO;
import interview.ai.config.model.vo.LlmSceneStatusVO;
import interview.ai.config.model.vo.LlmSceneVO;
import interview.ai.config.service.LlmSceneConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiVersion.V1 + "/admin/llm/scenes")
@Tag(name = "AI 场景执行参数管理（平台域）")
@RequiredArgsConstructor
@Validated
public class LlmSceneController {

    private final LlmSceneConfigService llmSceneConfigService;

    @Operation(summary = "分页查询 AI 场景执行参数")
    @RequirePermission(
            permissions = Perm.AdminLlm.SCENE_LIST,
            scope = PermissionScope.PLATFORM
    )
    @GetMapping
    public Result<LlmScenePageVO> pageScenes(@Valid LlmSceneQueryReq req) {
        return Result.success(llmSceneConfigService.pageScenes(req));
    }

    @Operation(summary = "查询 AI 场景执行参数详情")
    @RequirePermission(
            permissions = Perm.AdminLlm.SCENE_DETAIL,
            scope = PermissionScope.PLATFORM
    )
    @GetMapping("/{sceneCode}")
    public Result<LlmSceneVO> getScene(@PathVariable String sceneCode) {
        return Result.success(llmSceneConfigService.getScene(AiSceneCode.valueOf(sceneCode.toUpperCase())));
    }

    @Operation(summary = "更新 AI 场景执行参数（CAS 乐观锁）")
    @RequirePermission(
            permissions = Perm.AdminLlm.SCENE_UPDATE,
            scope = PermissionScope.PLATFORM
    )
    @PutMapping("/{sceneCode}")
    public Result<LlmSceneVO> updateScene(
            @PathVariable String sceneCode,
            @Valid @RequestBody LlmSceneUpdateReq req) {
        return Result.success(llmSceneConfigService.updateScene(AiSceneCode.valueOf(sceneCode.toUpperCase()), req));
    }

    @Operation(summary = "启停 AI 场景（CAS 乐观锁）")
    @RequirePermission(
            permissions = Perm.AdminLlm.SCENE_STATUS,
            scope = PermissionScope.PLATFORM
    )
    @PatchMapping("/{sceneCode}/status")
    public Result<LlmSceneStatusVO> updateSceneStatus(
            @PathVariable String sceneCode,
            @Valid @RequestBody LlmSceneStatusReq req) {
        return Result.success(llmSceneConfigService.updateSceneStatus(AiSceneCode.valueOf(sceneCode.toUpperCase()), req));
    }
}
