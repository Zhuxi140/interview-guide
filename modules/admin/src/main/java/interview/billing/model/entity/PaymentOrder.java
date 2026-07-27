package interview.billing.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import interview.billing.model.enums.PaymentOrderStatus;
import interview.framework.mybatis.JsonbStringTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@TableName(value = "payment_orders", autoResultMap = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrder implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;

    private Long enterpriseId;

    private Long userId;

    private Long skuId;

    private String idempotencyKey;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String skuSnapshot;

    private BigDecimal amount;

    private String currency;

    private Long tokensGranted;

    private String paymentProvider;

    private String providerPaymentId;

    private String paymentIntentIdempotencyKey;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String paymentIntentSnapshot;

    private String externalTransactionId;

    @Builder.Default
    private PaymentOrderStatus status = PaymentOrderStatus.PENDING;

    private OffsetDateTime expireTime;

    private OffsetDateTime paidAt;

    private OffsetDateTime cancelledAt;

    private OffsetDateTime expiredAt;

    private String statusReason;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;
}
