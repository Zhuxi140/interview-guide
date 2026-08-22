package interview.kyc.model.vo;

import interview.kyc.model.enums.KycMaterialType;
import interview.kyc.model.enums.KycStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 平台端实名认证审核详情。
 */
@Builder
@Schema(description = "平台端实名认证审核详情")
public record KycDetailVO(
        @Schema(description = "实名认证记录 ID")
        Long id,
        @Schema(description = "关联的 C 端求职者用户 ID")
        Long userId,
        @Schema(description = "脱敏后的真实姓名", example = "张**")
        String maskedRealName,
        @Schema(description = "脱敏后的身份证号", example = "3301**********1234")
        String maskedIdCardNo,
        @Schema(description = "认证状态", example = "PENDING")
        KycStatus authStatus,
        @Schema(description = "拒绝原因；仅 REJECTED 时返回", nullable = true)
        String rejectReason,
        @Schema(description = "已绑定的材料类型集合")
        List<KycMaterialType> materialTypes,
        @Schema(description = "最近一次提交时间")
        OffsetDateTime submitTime,
        @Schema(description = "审核处理时间；未审核时为空", nullable = true)
        OffsetDateTime auditTime
) {
}
