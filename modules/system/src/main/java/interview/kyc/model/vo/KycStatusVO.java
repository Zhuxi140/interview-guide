package interview.kyc.model.vo;

import interview.kyc.model.enums.KycStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * 本人实名认证审核状态。
 */
@Builder
@Schema(description = "本人实名认证审核状态")
public record KycStatusVO(
        @Schema(description = "实名认证记录 ID；未提交时为空", nullable = true)
        Long id,
        @Schema(description = "认证状态：NOT_SUBMITTED / PENDING / APPROVED / REJECTED",
                example = "PENDING")
        KycStatus authStatus,
        @Schema(description = "拒绝原因；仅 REJECTED 时返回", nullable = true)
        String rejectReason,
        @Schema(description = "最近一次提交时间；未提交时为空", nullable = true)
        OffsetDateTime submitTime,
        @Schema(description = "审核处理时间；未审核时为空", nullable = true)
        OffsetDateTime auditTime
) {
}
