package interview.system.tenant.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 企业联系电话验证流程启动结果。
 */
@Schema(description = "企业联系电话验证流程启动结果")
public record EnterpriseContactVerifyStartVO(
        @Schema(description = "验证流程 ID", example = "f6a8d9c0873f4bbd93d271cf88f639b1")
        String flowId,
        @Schema(description = "是否需要验证企业原联系电话", example = "true")
        boolean oldVerificationRequired
) {
}
