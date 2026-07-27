package interview.billing.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.billing.model.enums.PaymentOrderStatus;
import interview.billing.model.req.SimulatePaymentReq;
import interview.billing.model.vo.*;
import interview.billing.service.PaymentOrderService;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping(ApiVersion.V1 + "/admin/billing/orders")
@Tag(name = "充值订单管理（Admin）")
@RequiredArgsConstructor
@Validated
public class AdminOrderController {

    private final PaymentOrderService paymentOrderService;

    @Operation(summary = "平台分页查询全部充值订单")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.AdminBilling.ORDER_LIST, scope = PermissionScope.PLATFORM)
    @GetMapping
    public Result<IPage<OrderAdminListItemVO>> listOrders(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) PaymentOrderStatus status,
            @RequestParam(required = false) Long enterpriseId,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) OffsetDateTime startTime,
            @RequestParam(required = false) OffsetDateTime endTime) {
        return Result.success(paymentOrderService.pageAdminOrders(
                page, size, status, enterpriseId, orderNo, startTime, endTime));
    }

    @Operation(summary = "平台查询订单详情")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.AdminBilling.ORDER_DETAIL, scope = PermissionScope.PLATFORM)
    @GetMapping("/{orderId}")
    public Result<OrderAdminDetailVO> getOrder(@PathVariable Long orderId) {
        return Result.success(paymentOrderService.getAdminOrder(orderId));
    }

    @Operation(summary = "模拟支付（仅 dev/test 环境）")
    @RequirePermission(permissions = Perm.AdminBilling.ORDER_SIMULATE_PAYMENT, scope = PermissionScope.PLATFORM)
    @PostMapping("/{orderId}/simulate-payment")
    public Result<SimulatePaymentVO> simulatePayment(
            @PathVariable Long orderId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody SimulatePaymentReq req) {
        return Result.success(paymentOrderService.simulatePayment(orderId, idempotencyKey, req));
    }
}
