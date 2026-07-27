package interview.billing.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.billing.model.req.SkuCreateReq;
import interview.billing.model.req.SkuStatusReq;
import interview.billing.model.req.SkuUpdateReq;
import interview.billing.model.vo.*;
import interview.billing.service.SkuCatalogService;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiVersion.V1 + "/admin/billing/skus")
@Tag(name = "算力套餐管理（Admin）")
@RequiredArgsConstructor
@Validated
public class AdminSkuController {

    private final SkuCatalogService skuCatalogService;

    @Operation(summary = "平台查询全部 SKU（含上架/下架）")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.AdminBilling.SKU_LIST, scope = PermissionScope.PLATFORM)
    @GetMapping
    public Result<IPage<SkuListItemVO>> listSkus(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) Boolean isActive) {
        return Result.success(skuCatalogService.pageAdminSkus(page, size, isActive));
    }

    @Operation(summary = "平台查询 SKU 详情")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.AdminBilling.SKU_DETAIL, scope = PermissionScope.PLATFORM)
    @GetMapping("/{skuId}")
    public Result<SkuDetailVO> getSku(@PathVariable Long skuId) {
        return Result.success(skuCatalogService.getAdminSku(skuId));
    }

    @Operation(summary = "Admin 创建算力套餐")
    @RequirePermission(permissions = Perm.AdminBilling.SKU_CREATE, scope = PermissionScope.PLATFORM)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Result<SkuCreateVO> createSku(@Valid @RequestBody SkuCreateReq req) {
        return Result.success(skuCatalogService.createSku(req));
    }

    @Operation(summary = "Admin 部分更新算力套餐")
    @RequirePermission(permissions = Perm.AdminBilling.SKU_UPDATE, scope = PermissionScope.PLATFORM)
    @PatchMapping("/{skuId}")
    public Result<SkuUpdateVO> updateSku(
            @PathVariable Long skuId,
            @Valid @RequestBody SkuUpdateReq req) {
        return Result.success(skuCatalogService.updateSku(skuId, req));
    }

    @Operation(summary = "Admin 上架或下架套餐")
    @RequirePermission(permissions = Perm.AdminBilling.SKU_STATUS, scope = PermissionScope.PLATFORM)
    @PatchMapping("/{skuId}/status")
    public Result<SkuStatusVO> updateSkuStatus(
            @PathVariable Long skuId,
            @Valid @RequestBody SkuStatusReq req) {
        return Result.success(skuCatalogService.updateSkuStatus(skuId, req));
    }
}
