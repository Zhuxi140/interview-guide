package interview.billing.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

@TableName(value = "user_wallets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWallet implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long enterpriseId;

    @Builder.Default
    private Long balance = 0L;

    @Builder.Default
    private Long frozenBalance = 0L;

    @Builder.Default
    private Long totalRecharged = 0L;

    @Version
    @Builder.Default
    private Integer version = 0;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
