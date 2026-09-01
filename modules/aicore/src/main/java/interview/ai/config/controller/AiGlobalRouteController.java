package interview.ai.config.controller;

import interview.ai.config.model.req.AiGlobalRouteUpdateReq;
import interview.ai.config.model.vo.AiGlobalRouteListVO;
import interview.ai.config.model.vo.AiGlobalRouteVO;
import interview.ai.config.model.vo.AiRouteDetailVO;
import interview.ai.config.model.vo.AiRouteHealthVO;
import interview.ai.config.service.AiGlobalRouteService;
import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.AiModelType;
import interview.common.enums.PermissionScope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiVersion.V1 + "/admin/ai/routes")
@Tag(name = "AI 全局默认路由管理（平台域）")
@RequiredArgsConstructor
@Validated
public class AiGlobalRouteController {

    private final AiGlobalRouteService aiGlobalRouteService;

    @Operation(summary = "查询全部 AI 全局默认路由")
    @RequirePermission(
            permissions = Perm.AdminAi.ROUTE_LIST,
            scope = PermissionScope.PLATFORM
    )
    @GetMapping
    public Result<AiGlobalRouteListVO> listRoutes() {
        return Result.success(aiGlobalRouteService.listRoutes());
    }

    @Operation(summary = "查询指定模型类型的 AI 路由详情")
    @RequirePermission(
            permissions = Perm.AdminAi.ROUTE_LIST,
            scope = PermissionScope.PLATFORM
    )
    @GetMapping("/{modelType}")
    public Result<AiRouteDetailVO> getRoute(@PathVariable AiModelType modelType) {
        return Result.success(aiGlobalRouteService.getRoute(modelType));
    }

    @Operation(summary = "查询指定模型类型的 Provider 运行健康状态")
    @RequirePermission(
            permissions = Perm.AdminAi.ROUTE_LIST,
            scope = PermissionScope.PLATFORM
    )
    @GetMapping("/{modelType}/health")
    public Result<AiRouteHealthVO> getRouteHealth(@PathVariable AiModelType modelType) {
        return Result.success(aiGlobalRouteService.getRouteHealth(modelType));
    }

    @Operation(summary = "更新指定模型类型的 AI 全局默认路由（CAS 乐观锁）")
    @RequirePermission(
            permissions = Perm.AdminAi.ROUTE_UPDATE,
            scope = PermissionScope.PLATFORM
    )
    @PutMapping("/{modelType}")
    public Result<AiGlobalRouteVO> updateRoute(
            @PathVariable AiModelType modelType,
            @Valid @RequestBody AiGlobalRouteUpdateReq req) {
        return Result.success(aiGlobalRouteService.updateRoute(modelType, req));
    }
}
