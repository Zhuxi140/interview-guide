package interview.cert.model.vo;

import interview.cert.model.enums.CertStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * 企业资质认证审核状态。
 */
@Builder
@Schema(description = "企业资质认证审核状态")
public record EnterpriseCertStatusVO(
        @Schema(description = "认证记录 ID；未提交时为空", nullable = true)
        Long id,
        @Schema(description = "审核状态：NOT_SUBMITTED / PENDING / APPROVED / REJECTED",
                example = "PENDING")
        CertStatus auditStatus,
        @Schema(description = "拒绝原因；仅 REJECTED 时返回", nullable = true)
        String rejectReason,
        @Schema(description = "最近一次提交时间；未提交时为空", nullable = true)
        OffsetDateTime submitTime,
        @Schema(description = "审核处理时间；未审核时为空", nullable = true)
        OffsetDateTime auditTime
) {
}
