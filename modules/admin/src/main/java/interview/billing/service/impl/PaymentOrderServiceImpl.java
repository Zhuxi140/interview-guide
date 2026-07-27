package interview.billing.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.system.EnterpriseValidationApi;
import interview.billing.mapper.PaymentOrderMapper;
import interview.billing.model.entity.PaymentOrder;
import interview.billing.model.enums.PaymentOrderStatus;
import interview.billing.model.req.OrderCancelReq;
import interview.billing.model.req.OrderCreateReq;
import interview.billing.model.req.PaymentIntentReq;
import interview.billing.model.req.SimulatePaymentReq;
import interview.billing.model.vo.*;
import interview.billing.service.PaymentOrderService;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentOrderServiceImpl
        extends ServiceImpl<PaymentOrderMapper, PaymentOrder>
        implements PaymentOrderService {

    private final EnterpriseValidationApi enterpriseValidationApi;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public OrderCreateVO createOrder(Long enterpriseId, String idempotencyKey, OrderCreateReq req) {
        // TODO ① 校验 Idempotency-Key 非空且长度合法，查询 enterpriseId + idempotencyKey 对应的历史订单。
        // TODO ② 已存在订单时核对 skuId；请求一致则反序列化原 SKU 快照并返回原结果，不一致则抛出幂等键冲突。
        // TODO ③ 使用 LambdaQuery 查询未删除且已上架的 SKU，校验当前阶段 currency = CNY；不存在或已下架时拒绝下单。
        // TODO ④ 仅从服务端 SKU 构建 SkuSnapshotVO，并序列化 packageName、unitPrice、currency、tokensIncluded，不接受客户端金额。
        // TODO ⑤ 生成全局唯一 orderNo，按统一订单有效期计算 expireTime，构建 PENDING 的 PaymentOrder 实体并保存。
        // TODO ⑥ 唯一索引并发冲突时按 enterpriseId + idempotencyKey 回查；一致请求返回原订单，不一致请求抛出幂等冲突。
        // TODO ⑦ 通过 api 模块暴露的消息接口在当前事务登记订单过期任务，按 orderId 到期后原子执行 PENDING → EXPIRED。
        // TODO ⑧ 将订单实体及不可变 SKU 快照映射为 OrderCreateVO 返回。
        return null;
    }

    @Override
    @Transactional
    public PaymentIntentVO createPaymentIntent(Long enterpriseId, Long orderId,
                                                String idempotencyKey, PaymentIntentReq req) {
        // TODO ① 校验 Idempotency-Key 和支付渠道编码，渠道必须位于服务端允许列表，禁止客户端传入金额或订单快照。
        // TODO ② 使用 LambdaQuery 按 orderId + enterpriseId 查询订单，确保租户归属；不存在时抛出订单不存在异常。
        // TODO ③ 校验订单为 PENDING 且未过期；已到期时先按期望状态原子推进 EXPIRED，再返回订单已过期。
        // TODO ④ 检查 enterpriseId + paymentIntentIdempotencyKey：同键同订单同渠道时反序列化快照并返回，绑定其他请求时拒绝。
        // TODO ⑤ 订单已保存其他支付意图键时拒绝重复创建，防止同一订单并行生成多个支付意图。
        // TODO ⑥ 实现时拆分为“短事务预占 → 事务外调用支付渠道 → 短事务确认”，禁止持有数据库事务等待外部网络。
        // TODO ⑦ 预占阶段按 PENDING、未过期和幂等键条件保存 paymentProvider、paymentIntentIdempotencyKey，失败后回查状态。
        // TODO ⑧ 使用 orderNo 和幂等键调用支付适配器；渠道重试必须复用同一商户订单号，避免重复创建预支付单。
        // TODO ⑨ 确认阶段原子保存 providerPaymentId 及非敏感 paymentIntentSnapshot；订单状态或幂等键变化时禁止覆盖。
        // TODO ⑩ 从持久化快照构建 PaymentIntentVO，确保首次请求与幂等重放返回一致。
        return null;
    }

    @Override
    public IPage<OrderListItemVO> pageEnterpriseOrders(Long enterpriseId, Integer page, Integer size,
                                                        PaymentOrderStatus status,
                                                        OffsetDateTime startTime, OffsetDateTime endTime) {
        // 校验企业成员身份以及分页、时间范围。
        validateEnterpriseAccess(enterpriseId);
        validatePage(page, size);
        validateTimeRange(startTime, endTime);

        // 订单查询始终限定企业，并按创建时间和主键稳定倒序。
        IPage<PaymentOrder> orderPage = lambdaQuery()
                .select(PaymentOrder::getId, PaymentOrder::getOrderNo,
                        PaymentOrder::getAmount, PaymentOrder::getCurrency,
                        PaymentOrder::getTokensGranted, PaymentOrder::getStatus,
                        PaymentOrder::getExpireTime, PaymentOrder::getCreatedAt)
                .eq(PaymentOrder::getEnterpriseId, enterpriseId)
                .eq(status != null, PaymentOrder::getStatus, status)
                .ge(startTime != null, PaymentOrder::getCreatedAt, startTime)
                .le(endTime != null, PaymentOrder::getCreatedAt, endTime)
                .orderByDesc(PaymentOrder::getCreatedAt)
                .orderByDesc(PaymentOrder::getId)
                .page(new Page<>(page, size));

        // 将实体分页转换为企业端列表响应。
        return orderPage.convert(order -> new OrderListItemVO(
                order.getId(),
                order.getOrderNo(),
                order.getAmount(),
                order.getCurrency(),
                order.getTokensGranted(),
                order.getStatus(),
                order.getExpireTime(),
                order.getCreatedAt()
        ));
    }

    @Override
    public OrderDetailVO getEnterpriseOrder(Long enterpriseId, Long orderId) {
        // 同时校验成员身份和订单租户归属，避免跨企业读取。
        validateEnterpriseAccess(enterpriseId);
        PaymentOrder order = lambdaQuery()
                .eq(PaymentOrder::getId, orderId)
                .eq(PaymentOrder::getEnterpriseId, enterpriseId)
                .one();
        if (order == null) {
            throw new BusinessException(ErrorCode.PAYMENT_ORDER_NOT_FOUND);
        }

        // 解析下单时保存的不可变 SKU 快照并组装详情。
        return toEnterpriseDetail(order, readSkuSnapshot(order));
    }

    @Override
    @Transactional
    public OrderUpdateVO cancelOrder(Long enterpriseId, Long orderId, OrderCancelReq req) {
        // TODO ① 校验 expectedStatus，只允许以 PENDING 作为取消前置状态。
        // TODO ② 按 id + enterpriseId 查询订单并校验租户归属；已是 CANCELLED 时幂等返回原结果。
        // TODO ③ PAID、EXPIRED 等终态禁止取消；订单已到期但仍为 PENDING 时应先推进 EXPIRED。
        // TODO ④ 使用 LambdaUpdate 按 id + enterpriseId + status=PENDING + expireTime>now 原子更新为 CANCELLED。
        // TODO ⑤ 同时显式写入 cancelledAt、statusReason、updatedAt、traceId 等非实体更新不会自动填充的审计字段。
        // TODO ⑥ 更新零行时回查最新状态：CANCELLED 返回原结果，其余状态按并发后的真实结果返回业务异常。
        // TODO ⑦ 将最终订单状态和更新时间映射为 OrderUpdateVO；后续过期任务看到非 PENDING 时直接忽略。
        return null;
    }

    @Override
    public IPage<OrderAdminListItemVO> pageAdminOrders(Integer page, Integer size,
                                                        PaymentOrderStatus status,
                                                        Long enterpriseId, String orderNo,
                                                        OffsetDateTime startTime, OffsetDateTime endTime) {
        // 校验分页和时间范围，平台查询根据可选条件组合筛选。
        validatePage(page, size);
        validateTimeRange(startTime, endTime);
        boolean hasOrderNo = orderNo != null && !orderNo.isBlank();
        IPage<PaymentOrder> orderPage = lambdaQuery()
                .select(PaymentOrder::getId, PaymentOrder::getOrderNo,
                        PaymentOrder::getEnterpriseId, PaymentOrder::getAmount,
                        PaymentOrder::getCurrency, PaymentOrder::getTokensGranted,
                        PaymentOrder::getStatus, PaymentOrder::getCreatedAt)
                .eq(status != null, PaymentOrder::getStatus, status)
                .eq(enterpriseId != null, PaymentOrder::getEnterpriseId, enterpriseId)
                .eq(hasOrderNo, PaymentOrder::getOrderNo,
                        hasOrderNo ? orderNo.trim() : null)
                .ge(startTime != null, PaymentOrder::getCreatedAt, startTime)
                .le(endTime != null, PaymentOrder::getCreatedAt, endTime)
                .orderByDesc(PaymentOrder::getCreatedAt)
                .orderByDesc(PaymentOrder::getId)
                .page(new Page<>(page, size));

        // 转换为管理端订单列表。
        return orderPage.convert(order -> new OrderAdminListItemVO(
                order.getId(),
                order.getOrderNo(),
                order.getEnterpriseId(),
                order.getAmount(),
                order.getCurrency(),
                order.getTokensGranted(),
                order.getStatus(),
                order.getCreatedAt()
        ));
    }

    @Override
    public OrderAdminDetailVO getAdminOrder(Long orderId) {
        // 平台按主键查询订单，不存在时返回统一业务异常。
        PaymentOrder order = getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.PAYMENT_ORDER_NOT_FOUND);
        }

        // 解析 SKU 快照并返回平台详情。
        return toAdminDetail(order, readSkuSnapshot(order));
    }

    @Override
    @Transactional
    public SimulatePaymentVO simulatePayment(Long orderId, String idempotencyKey, SimulatePaymentReq req) {
        // TODO ① 仅在 dev/test Profile 注册并再次校验运行环境，生产环境必须拒绝模拟支付。
        // TODO ② 校验 Idempotency-Key 与 externalTransactionId，渠道流水号必须全局唯一。
        // TODO ③ 查询订单：已 PAID 且渠道流水一致时幂等返回；已 PAID 但流水不同或处于其他终态时拒绝处理。
        // TODO ④ 校验 PENDING 且 expireTime>now；到期订单原子推进 EXPIRED，不允许模拟支付覆盖终态。
        // TODO ⑤ 生成稳定充值命令键并检查 RECHARGE 账本；已有成功流水时直接返回原结果，防止钱包重复入账。
        // TODO ⑥ 复用支付成功的领域处理方法，以 id + status=PENDING + expireTime>now 原子推进 PAID 并写入 paidAt、渠道信息。
        // TODO ⑦ 锁定或使用 Mapper XML 原子更新企业钱包：balance、totalRecharged 增加 tokensGranted，version 同步递增并取得余额快照。
        // TODO ⑧ 在同一事务写入 RECHARGE 权威流水，referenceType=PAYMENT_ORDER、referenceId=orderId、commandId 保证幂等。
        // TODO ⑨ 订单推进、钱包充值或账本写入任一步失败均回滚；唯一键冲突时回查并判断是否为同一幂等结果。
        // TODO ⑩ 返回 SimulatePaymentVO；正式支付回调必须复用相同入账逻辑，仅额外执行渠道验签和防重放。
        return null;
    }

    private void validateEnterpriseAccess(Long enterpriseId) {
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());
    }

    private void validatePage(Integer page, Integer size) {
        if (page == null || page < 1 || size == null || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.PAGE_PARAM_INVALID);
        }
    }

    private void validateTimeRange(OffsetDateTime startTime, OffsetDateTime endTime) {
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new BusinessException(ErrorCode.TIME_RANGE_INVALID);
        }
    }

    private SkuSnapshotVO readSkuSnapshot(PaymentOrder order) {
        if (order.getSkuSnapshot() == null || order.getSkuSnapshot().isBlank()) {
            throw new BusinessException(ErrorCode.JSON_TO_OBJECT_ERROR);
        }
        try {
            return objectMapper.readValue(order.getSkuSnapshot(), SkuSnapshotVO.class);
        } catch (Exception exception) {
            log.error("充值订单 SKU 快照解析失败，orderId: {}", order.getId(), exception);
            throw new BusinessException(ErrorCode.JSON_TO_OBJECT_ERROR);
        }
    }

    private OrderDetailVO toEnterpriseDetail(PaymentOrder order, SkuSnapshotVO snapshot) {
        return new OrderDetailVO(
                order.getId(),
                order.getOrderNo(),
                snapshot,
                order.getAmount(),
                order.getCurrency(),
                order.getTokensGranted(),
                order.getStatus(),
                order.getPaymentProvider(),
                order.getExternalTransactionId(),
                order.getExpireTime(),
                order.getPaidAt(),
                order.getCancelledAt(),
                order.getExpiredAt(),
                order.getCreatedAt()
        );
    }

    private OrderAdminDetailVO toAdminDetail(PaymentOrder order, SkuSnapshotVO snapshot) {
        return new OrderAdminDetailVO(
                order.getId(),
                order.getOrderNo(),
                order.getEnterpriseId(),
                snapshot,
                order.getAmount(),
                order.getCurrency(),
                order.getTokensGranted(),
                order.getStatus(),
                order.getPaymentProvider(),
                order.getExternalTransactionId(),
                order.getExpireTime(),
                order.getPaidAt(),
                order.getCancelledAt(),
                order.getExpiredAt(),
                order.getCreatedAt()
        );
    }
}
