package interview.billing.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.billing.model.enums.PaymentOrderStatus;
import interview.billing.model.req.OrderCancelReq;
import interview.billing.model.req.OrderCreateReq;
import interview.billing.model.req.PaymentIntentReq;
import interview.billing.model.vo.*;
import interview.billing.service.PaymentOrderService;
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
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequireActiveEnterprise
@RequestMapping(ApiVersion.V1 + "/enterprises/{enterpriseId}/billing/orders")
@Tag(name = "充值订单（企业端）")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final PaymentOrderService paymentOrderService;

    @Operation(summary = "企业创建充值订单")
    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.BillingOrder.CREATE, scope = PermissionScope.ENTERPRISE)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Result<OrderCreateVO> createOrder(
            @PathVariable Long enterpriseId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody OrderCreateReq req) {
        return Result.success(paymentOrderService.createOrder(enterpriseId, idempotencyKey, req));
    }

    @Operation(summary = "幂等创建或获取支付意图")
    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.BillingOrder.PAY, scope = PermissionScope.ENTERPRISE)
    @PostMapping("/{orderId}/payment-intents")
    public Result<PaymentIntentVO> createPaymentIntent(
            @PathVariable Long enterpriseId,
            @PathVariable Long orderId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PaymentIntentReq req) {
        return Result.success(paymentOrderService.createPaymentIntent(enterpriseId, orderId, idempotencyKey, req));
    }

    @Operation(summary = "企业查询自己的充值订单（分页）")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.BillingOrder.LIST, scope = PermissionScope.ENTERPRISE)
    @GetMapping
    public Result<IPage<OrderListItemVO>> listOrders(
            @PathVariable Long enterpriseId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) PaymentOrderStatus status,
            @RequestParam(required = false) OffsetDateTime startTime,
            @RequestParam(required = false) OffsetDateTime endTime) {
        return Result.success(paymentOrderService.pageEnterpriseOrders(
                enterpriseId, page, size, status, startTime, endTime));
    }

    @Operation(summary = "企业查询自己的订单详情")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.BillingOrder.DETAIL, scope = PermissionScope.ENTERPRISE)
    @GetMapping("/{orderId}")
    public Result<OrderDetailVO> getOrder(
            @PathVariable Long enterpriseId,
            @PathVariable Long orderId) {
        return Result.success(paymentOrderService.getEnterpriseOrder(enterpriseId, orderId));
    }

    @Operation(summary = "企业幂等取消自己的未支付订单")
    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.BillingOrder.CANCEL, scope = PermissionScope.ENTERPRISE)
    @PostMapping("/{orderId}/cancel")
    public Result<OrderUpdateVO> cancelOrder(
            @PathVariable Long enterpriseId,
            @PathVariable Long orderId,
            @Valid @RequestBody OrderCancelReq req) {
        return Result.success(paymentOrderService.cancelOrder(enterpriseId, orderId, req));
    }
}
