package interview.cert.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 营业执照短期上传凭证。
 */
@Builder
@Schema(description = "营业执照短期上传凭证")
public record EnterpriseCertUploadTicketVO(
        @Schema(description = "材料令牌：即对象存储键，提交资质认证时用于绑定执照",
                example = "ENTERPRISE_CERT/2026/08/22/3f2a9d8c_business-license.jpg")
        String materialToken,
        @Schema(description = "短期直传地址；直传通道接入前为空，客户端需经由服务端中转上传",
                nullable = true)
        String uploadUrl,
        @Schema(description = "凭证有效期（秒）", example = "300")
        Long expiresInSeconds
) {
}
