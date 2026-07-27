package interview.billing.model.vo;

import interview.billing.model.enums.WalletTransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "钱包流水列表项")
public record WalletTransactionListItemVO(
        @Schema(description = "流水 ID")
        Long id,

        @Schema(description = "钱包命令幂等键")
        String commandId,

        @Schema(description = "可用余额变化量")
        Long balanceChange,

        @Schema(description = "冻结余额变化量")
        Long frozenChange,

        @Schema(description = "流水类型（FREEZE / UNFREEZE / CONSUME / RECHARGE）")
        WalletTransactionType type,

        @Schema(description = "关联资源类型")
        String referenceType,

        @Schema(description = "关联资源主键")
        Long referenceId,

        @Schema(description = "变更后可用余额快照")
        Long balanceAfter,

        @Schema(description = "变更后冻结余额快照")
        Long frozenAfter,

        @Schema(description = "流水生成时间")
        OffsetDateTime createdAt,

        @Schema(description = "调用链 ID")
        String traceId
) {
}
