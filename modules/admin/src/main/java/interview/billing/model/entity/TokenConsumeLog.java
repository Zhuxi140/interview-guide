package interview.billing.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import interview.common.enums.BizType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

@TableName(value = "token_consume_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenConsumeLog implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long enterpriseId;

    private Long userId;

    private Long walletTransactionId;

    private String commandId;

    private BizType bizType;

    private Long bizId;

    @Builder.Default
    private Integer attemptNo = 1;

    private Long tokensConsumed;

    private Long balanceAfter;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;
}
