package interview.billing.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.billing.model.vo.WalletTransactionListItemVO;
import interview.billing.model.vo.WalletVO;
import interview.billing.model.enums.WalletTransactionType;
import interview.billing.service.UserWalletService;
import interview.billing.service.WalletTransactionService;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequirePermission;
import interview.common.annonate.RequireActiveEnterprise;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequireActiveEnterprise
@RequestMapping(ApiVersion.V1 + "/enterprises/{enterpriseId}/billing/wallet")
@Tag(name = "钱包查询（企业端）")
@RequiredArgsConstructor
@Validated
public class WalletController {

    private final UserWalletService userWalletService;
    private final WalletTransactionService walletTransactionService;

    @Operation(summary = "查询企业钱包信息")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.BillingWallet.DETAIL, scope = PermissionScope.ENTERPRISE)
    @GetMapping
    public Result<WalletVO> getWallet(@PathVariable Long enterpriseId) {
        return Result.success(userWalletService.getWallet(enterpriseId));
    }

    @Operation(summary = "查询钱包权威账本（分页）")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.BillingWallet.TRANSACTIONS, scope = PermissionScope.ENTERPRISE)
    @GetMapping("/transactions")
    public Result<IPage<WalletTransactionListItemVO>> listTransactions(
            @PathVariable Long enterpriseId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) WalletTransactionType type,
            @RequestParam(required = false) OffsetDateTime startTime,
            @RequestParam(required = false) OffsetDateTime endTime) {
        return Result.success(walletTransactionService.pageTransactions(
                enterpriseId, page, size, type, startTime, endTime));
    }
}
