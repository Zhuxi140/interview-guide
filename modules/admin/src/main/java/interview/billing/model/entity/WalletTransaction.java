package interview.billing.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import interview.billing.model.enums.WalletTransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

@TableName(value = "wallet_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long enterpriseId;

    private Long walletId;

    private String commandId;

    private Long parentTransactionId;

    private Long balanceChange;

    @Builder.Default
    private Long frozenChange = 0L;

    private WalletTransactionType type;

    private String referenceType;

    private Long referenceId;

    private Long balanceAfter;

    private Long frozenAfter;

    private Long operatorUserId;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;
}
