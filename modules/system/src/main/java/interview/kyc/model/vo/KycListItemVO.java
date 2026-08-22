package interview.kyc.model.vo;

import interview.kyc.model.enums.KycStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * 平台端实名认证审核列表项。
 */
@Builder
@Schema(description = "平台端实名认证审核列表项")
public record KycListItemVO(
        @Schema(description = "实名认证记录 ID")
        Long id,
        @Schema(description = "关联的 C 端求职者用户 ID")
        Long userId,
        @Schema(description = "脱敏后的真实姓名", example = "张**")
        String maskedRealName,
        @Schema(description = "认证状态", example = "PENDING")
        KycStatus authStatus,
        @Schema(description = "最近一次提交时间")
        OffsetDateTime submitTime
) {
}
