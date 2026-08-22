package interview.cert.model.vo;

import interview.cert.model.enums.CertStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * 平台端企业认证审核详情。
 */
@Builder
@Schema(description = "平台端企业认证审核详情")
public record EnterpriseCertDetailVO(
        @Schema(description = "认证记录 ID")
        Long id,
        @Schema(description = "企业租户 ID")
        Long enterpriseId,
        @Schema(description = "企业法定名称（提交时快照）", example = "杭州示例科技有限公司")
        String companyName,
        @Schema(description = "脱敏后的统一社会信用代码", example = "9133**********01X")
        String creditCodeMasked,
        @Schema(description = "脱敏后的法定代表人姓名", example = "张**")
        String legalPersonMasked,
        @Schema(description = "审核状态", example = "PENDING")
        CertStatus auditStatus,
        @Schema(description = "拒绝原因；仅 REJECTED 时返回", nullable = true)
        String rejectReason,
        @Schema(description = "最近一次提交时间")
        OffsetDateTime submitTime,
        @Schema(description = "审核处理时间；未审核时为空", nullable = true)
        OffsetDateTime auditTime
) {
}
