package interview.cert.model.vo;

import interview.cert.model.enums.CertStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * 平台端企业资质审核结果。
 */
@Builder
@Schema(description = "平台端企业资质审核结果")
public record EnterpriseCertAuditVO(
        @Schema(description = "认证记录 ID")
        Long id,
        @Schema(description = "审核后状态：APPROVED / REJECTED", example = "APPROVED")
        CertStatus auditStatus,
        @Schema(description = "审核处理时间")
        OffsetDateTime auditTime
) {
}
