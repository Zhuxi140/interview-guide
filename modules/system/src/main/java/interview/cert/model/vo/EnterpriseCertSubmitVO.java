package interview.cert.model.vo;

import interview.cert.model.enums.CertStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * 提交企业资质认证结果。
 */
@Builder
@Schema(description = "提交企业资质认证结果")
public record EnterpriseCertSubmitVO(
        @Schema(description = "认证记录 ID")
        Long id,
        @Schema(description = "企业租户 ID")
        Long enterpriseId,
        @Schema(description = "审核状态", example = "PENDING")
        CertStatus auditStatus,
        @Schema(description = "资料提交时间")
        OffsetDateTime submitTime
) {
}
