package interview.system.tenant.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 企业联系电话更新结果。
 */
@Schema(description = "企业联系电话更新结果")
public record EnterpriseContactPhoneUpdateVO(
        @Schema(description = "企业 ID", example = "10001")
        Long id,
        @Schema(description = "企业联系电话", example = "13800138000")
        String contactPhone
) {
}
