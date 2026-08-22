package interview.cert.model.vo;

import interview.cert.model.enums.CertStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * 平台端企业认证审核列表项。
 */
@Builder
@Schema(description = "平台端企业认证审核列表项")
public record EnterpriseCertListItemVO(
        @Schema(description = "认证记录 ID")
        Long id,
        @Schema(description = "企业租户 ID")
        Long enterpriseId,
        @Schema(description = "企业法定名称（提交时快照）", example = "杭州示例科技有限公司")
        String companyName,
        @Schema(description = "审核状态", example = "PENDING")
        CertStatus auditStatus,
        @Schema(description = "最近一次提交时间")
        OffsetDateTime submitTime
) {
}
