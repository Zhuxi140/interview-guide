package interview.billing.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WalletTransactionType {

    FREEZE("冻结"),
    UNFREEZE("解冻"),
    CONSUME("消费"),
    RECHARGE("充值");

    private final String message;
}
