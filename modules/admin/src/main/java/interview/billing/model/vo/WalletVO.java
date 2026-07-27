package interview.billing.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "企业算力钱包")
public record WalletVO(
        @Schema(description = "钱包 ID")
        Long id,

        @Schema(description = "企业 ID")
        Long enterpriseId,

        @Schema(description = "账户可用算力余额")
        Long balance,

        @Schema(description = "已冻结待结算的算力额度")
        Long frozenBalance,

        @Schema(description = "历史累计充值算力总额")
        Long totalRecharged,

        @Schema(description = "业务版本号")
        Integer version,

        @Schema(description = "最后更新时间")
        OffsetDateTime updatedAt
) {
}
