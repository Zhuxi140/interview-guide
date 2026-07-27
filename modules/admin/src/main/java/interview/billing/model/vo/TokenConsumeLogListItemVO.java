package interview.billing.model.vo;

import interview.common.enums.BizType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "算力消耗明细列表项")
public record TokenConsumeLogListItemVO(
        @Schema(description = "消耗 ID")
        Long id,

        @Schema(description = "对应钱包 CONSUME 流水 ID")
        Long walletTransactionId,

        @Schema(description = "操作用户 ID")
        Long userId,

        @Schema(description = "业务类型")
        BizType bizType,

        @Schema(description = "关联业务主键 ID")
        Long bizId,

        @Schema(description = "同一业务的模型调用尝试序号")
        Integer attemptNo,

        @Schema(description = "本次消耗算力数")
        Long tokensConsumed,

        @Schema(description = "变动后账户算力余额")
        Long balanceAfter,

        @Schema(description = "流水生成时间")
        OffsetDateTime createdAt,

        @Schema(description = "调用链 ID")
        String traceId
) {
}
