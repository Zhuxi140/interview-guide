package interview.billing.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.billing.model.entity.PaymentOrder;
import interview.billing.model.req.OrderCancelReq;
import interview.billing.model.req.OrderCreateReq;
import interview.billing.model.req.PaymentIntentReq;
import interview.billing.model.req.SimulatePaymentReq;
import interview.billing.model.enums.PaymentOrderStatus;
import interview.billing.model.vo.*;

import java.time.OffsetDateTime;

public interface PaymentOrderService extends IService<PaymentOrder> {

    /**
     * 创建企业充值订单
     * @param enterpriseId 企业 ID
     * @param idempotencyKey 幂等键
     * @param req 创建订单请求
     * @return 创建的订单
     */
    OrderCreateVO createOrder(Long enterpriseId, String idempotencyKey, OrderCreateReq req);

    /**
     * 创建或获取支付意图
     * @param enterpriseId 企业 ID
     * @param orderId 订单 ID
     * @param idempotencyKey 幂等键
     * @param req 支付意图请求
     * @return 支付意图
     */
    PaymentIntentVO createPaymentIntent(Long enterpriseId, Long orderId, String idempotencyKey, PaymentIntentReq req);

    /**
     * 分页查询企业充值订单
     * @param enterpriseId 企业 ID
     * @param page 当前页
     * @param size 每页数量
     * @param status 订单状态
     * @param startTime 创建时间起点
     * @param endTime 创建时间终点
     * @return 企业订单分页
     */
    IPage<OrderListItemVO> pageEnterpriseOrders(Long enterpriseId, Integer page, Integer size,
                                                 PaymentOrderStatus status,
                                                 OffsetDateTime startTime, OffsetDateTime endTime);

    /**
     * 查询企业充值订单详情
     * @param enterpriseId 企业 ID
     * @param orderId 订单 ID
     * @return 企业订单详情
     */
    OrderDetailVO getEnterpriseOrder(Long enterpriseId, Long orderId);

    /**
     * 取消企业未支付订单
     * @param enterpriseId 企业 ID
     * @param orderId 订单 ID
     * @param req 取消订单请求
     * @return 订单更新结果
     */
    OrderUpdateVO cancelOrder(Long enterpriseId, Long orderId, OrderCancelReq req);

    /**
     * 分页查询平台充值订单
     * @param page 当前页
     * @param size 每页数量
     * @param status 订单状态
     * @param enterpriseId 企业 ID
     * @param orderNo 商户订单号
     * @param startTime 创建时间起点
     * @param endTime 创建时间终点
     * @return 平台订单分页
     */
    IPage<OrderAdminListItemVO> pageAdminOrders(Integer page, Integer size, PaymentOrderStatus status,
                                                 Long enterpriseId, String orderNo,
                                                 OffsetDateTime startTime, OffsetDateTime endTime);

    /**
     * 查询平台充值订单详情
     * @param orderId 订单 ID
     * @return 平台订单详情
     */
    OrderAdminDetailVO getAdminOrder(Long orderId);

    /**
     * 模拟订单支付成功
     * @param orderId 订单 ID
     * @param idempotencyKey 幂等键
     * @param req 模拟支付请求
     * @return 模拟支付结果
     */
    SimulatePaymentVO simulatePayment(Long orderId, String idempotencyKey, SimulatePaymentReq req);
}
