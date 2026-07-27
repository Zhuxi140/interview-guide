package interview.billing.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentOrderStatus {

    PENDING("待支付"),
    PAID("已支付"),
    CANCELLED("已取消"),
    EXPIRED("已过期");

    private final String message;
}
