package interview.billing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;

import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;

import interview.api.system.EnterpriseValidationApi;
import interview.billing.mapper.PaymentOrderMapper;
import interview.billing.model.entity.PaymentOrder;
import interview.billing.model.enums.PaymentOrderStatus;
import interview.billing.model.req.OrderCancelReq;
import interview.billing.model.vo.OrderUpdateVO;
import interview.common.enums.ErrorCode;
import interview.common.enums.UserType;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PaymentOrderServiceImplTest {

    private static final long ENTERPRISE_ID = 10L;
    private static final long ORDER_ID = 8801L;
    private static final long CURRENT_USER_ID = 1L;

    @Mock
    private EnterpriseValidationApi enterpriseValidationApi;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private PaymentOrderMapper paymentOrderMapper;

    private PaymentOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PaymentOrderServiceImpl(enterpriseValidationApi, objectMapper);
        ReflectionTestUtils.setField(service, "baseMapper", paymentOrderMapper);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "PaymentOrder"),
                PaymentOrder.class);
        AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(CURRENT_USER_ID)
                .userType(UserType.ENTERPRISE_USER)
                .build());
    }

    @AfterEach
    void tearDown() {
        AuthContext.remove();
    }

    @Test
    void cancelOrder_shouldRejectUnexpectedStatus() {
        // 只允许以 PENDING 为前置状态：连订单都不该查。
        OrderCancelReq req = cancelReq(PaymentOrderStatus.PAID);

        BusinessException exception = assertBusinessException(() -> service.cancelOrder(
                ENTERPRISE_ID, ORDER_ID, req));

        assertEquals(ErrorCode.PARAM_VALID_ERROR.getCode(), exception.getCode());
        verify(paymentOrderMapper, never()).selectOne(any());
    }

    @Test
    void cancelOrder_shouldRejectUnknownOrder() {
        // 查询条件含 enterpriseId，未命中即视为不存在（同时防跨企业取消）。
        when(paymentOrderMapper.selectOne(any())).thenReturn(null);

        BusinessException exception = assertBusinessException(() -> service.cancelOrder(
                ENTERPRISE_ID, ORDER_ID, cancelReq(PaymentOrderStatus.PENDING)));

        assertEquals(ErrorCode.PAYMENT_ORDER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void cancelOrder_shouldReplayWhenAlreadyCancelled() {
        // 已取消：幂等重放既有结果，不再发起条件更新。
        when(paymentOrderMapper.selectOne(any()))
                .thenReturn(order(PaymentOrderStatus.CANCELLED, OffsetDateTime.now().plusHours(1)));

        OrderUpdateVO result = service.cancelOrder(
                ENTERPRISE_ID, ORDER_ID, cancelReq(PaymentOrderStatus.PENDING));

        assertEquals(ORDER_ID, result.id());
        assertEquals(PaymentOrderStatus.CANCELLED, result.status());
        verify(paymentOrderMapper, never()).update(isNull(), any());
    }

    @Test
    void cancelOrder_shouldRejectPaidOrder() {
        // 已支付是终态，不可取消。
        when(paymentOrderMapper.selectOne(any()))
                .thenReturn(order(PaymentOrderStatus.PAID, OffsetDateTime.now().plusHours(1)));

        BusinessException exception = assertBusinessException(() -> service.cancelOrder(
                ENTERPRISE_ID, ORDER_ID, cancelReq(PaymentOrderStatus.PENDING)));

        assertEquals(ErrorCode.PAYMENT_ORDER_ALREADY_PAID.getCode(), exception.getCode());
    }

    @Test
    void cancelOrder_shouldExpireLapsedPendingOrderBeforeRejecting() {
        // 仍是 PENDING 但已过到期时间：先原子推进 EXPIRED，再按过期拒绝。
        when(paymentOrderMapper.selectOne(any()))
                .thenReturn(order(PaymentOrderStatus.PENDING, OffsetDateTime.now().minusMinutes(1)));
        when(paymentOrderMapper.update(isNull(), any())).thenReturn(1);

        BusinessException exception = assertBusinessException(() -> service.cancelOrder(
                ENTERPRISE_ID, ORDER_ID, cancelReq(PaymentOrderStatus.PENDING)));

        assertEquals(ErrorCode.PAYMENT_ORDER_EXPIRED.getCode(), exception.getCode());
        verify(paymentOrderMapper).update(isNull(), any());
    }

    @Test
    void cancelOrder_shouldCancelPendingOrderAtomically() {
        // 正常路径：条件更新命中一行，返回 CANCELLED 与本次时间基准。
        when(paymentOrderMapper.selectOne(any()))
                .thenReturn(order(PaymentOrderStatus.PENDING, OffsetDateTime.now().plusHours(1)));
        when(paymentOrderMapper.update(isNull(), any())).thenReturn(1);

        OrderUpdateVO result = service.cancelOrder(
                ENTERPRISE_ID, ORDER_ID, cancelReq(PaymentOrderStatus.PENDING));

        assertEquals(ORDER_ID, result.id());
        assertEquals(PaymentOrderStatus.CANCELLED, result.status());
        assertNotNull(result.updatedAt());
    }

    @Test
    void cancelOrder_shouldReplayWhenConcurrentCancelWon() {
        // 条件更新零行意味着并发者已改状态：回查确认是对手取消，按幂等返回。
        when(paymentOrderMapper.selectOne(any()))
                .thenReturn(order(PaymentOrderStatus.PENDING, OffsetDateTime.now().plusHours(1)))
                .thenReturn(order(PaymentOrderStatus.CANCELLED, OffsetDateTime.now()));
        when(paymentOrderMapper.update(isNull(), any())).thenReturn(0);

        OrderUpdateVO result = service.cancelOrder(
                ENTERPRISE_ID, ORDER_ID, cancelReq(PaymentOrderStatus.PENDING));

        assertEquals(PaymentOrderStatus.CANCELLED, result.status());
    }

    @Test
    void cancelOrder_shouldReportConflictWhenZeroRowButStillPending() {
        // 零行后回查仍是未过期的 PENDING：条件更新与快照不一致，必须给出明确冲突，
        // 不能返回 null 让前端拿到空响应体。
        OffsetDateTime notExpired = OffsetDateTime.now().plusHours(1);
        when(paymentOrderMapper.selectOne(any()))
                .thenReturn(order(PaymentOrderStatus.PENDING, notExpired))
                .thenReturn(order(PaymentOrderStatus.PENDING, notExpired));
        when(paymentOrderMapper.update(isNull(), any())).thenReturn(0);

        BusinessException exception = assertBusinessException(() -> service.cancelOrder(
                ENTERPRISE_ID, ORDER_ID, cancelReq(PaymentOrderStatus.PENDING)));

        assertEquals(ErrorCode.PAYMENT_ORDER_STATUS_CONFLICT.getCode(), exception.getCode());
    }

    private OrderCancelReq cancelReq(PaymentOrderStatus expectedStatus) {
        OrderCancelReq req = new OrderCancelReq();
        req.setExpectedStatus(expectedStatus);
        return req;
    }

    private PaymentOrder order(PaymentOrderStatus status, OffsetDateTime expireTime) {
        PaymentOrder order = new PaymentOrder();
        order.setId(ORDER_ID);
        order.setEnterpriseId(ENTERPRISE_ID);
        order.setStatus(status);
        order.setExpireTime(expireTime);
        order.setUpdatedAt(OffsetDateTime.now());
        return order;
    }

    private BusinessException assertBusinessException(Executable executable) {
        return assertThrows(BusinessException.class, executable);
    }
}
