package interview.system.tenant.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 企业联系邮箱更新结果。
 */
@Schema(description = "企业联系邮箱更新结果")
public record EnterpriseContactEmailUpdateVO(
        @Schema(description = "企业 ID", example = "10001")
        Long id,
        @Schema(description = "企业联系邮箱", example = "hr@example.com")
        String contactEmail
) {
}
