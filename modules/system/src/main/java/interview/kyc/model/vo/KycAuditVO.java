package interview.kyc.model.vo;

import interview.kyc.model.enums.KycStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * 平台端实名认证审核结果。
 */
@Builder
@Schema(description = "平台端实名认证审核结果")
public record KycAuditVO(
        @Schema(description = "实名认证记录 ID")
        Long id,
        @Schema(description = "审核后认证状态：APPROVED / REJECTED", example = "APPROVED")
        KycStatus authStatus,
        @Schema(description = "审核处理时间")
        OffsetDateTime auditTime
) {
}
