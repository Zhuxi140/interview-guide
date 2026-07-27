package interview.system.tenant.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 企业联系电话验证令牌结果。
 */
@Schema(description = "企业联系电话验证令牌结果")
public record EnterpriseContactVerifyTokenVO(
        @Schema(description = "一次性安全操作令牌")
        String secureActionToken
) {
}
