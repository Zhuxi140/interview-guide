package interview.billing.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.billing.model.vo.SkuVO;
import interview.billing.service.SkuCatalogService;
import interview.common.annonate.MaxRiskLevel;
import interview.common.constant.ApiVersion;
import interview.common.constant.Result;
import interview.common.enums.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiVersion.V1 + "/billing/skus")
@Tag(name = "算力套餐（公开）")
@RequiredArgsConstructor
@Validated
public class SkuController {

    private final SkuCatalogService skuCatalogService;

    @Operation(summary = "查询当前可购买的算力套餐（分页）")
    @MaxRiskLevel(RiskLevel.HIGH_RISK)
    @GetMapping
    public Result<IPage<SkuVO>> listSkus(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size) {
        return Result.success(skuCatalogService.pagePublicSkus(page, size));
    }

    @Operation(summary = "查询可购买套餐详情")
    @MaxRiskLevel(RiskLevel.HIGH_RISK)
    @GetMapping("/{skuId}")
    public Result<SkuVO> getSku(@PathVariable Long skuId) {
        return Result.success(skuCatalogService.getPublicSku(skuId));
    }
}
