package interview.kyc.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * KYC 材料短期上传凭证。
 */
@Builder
@Schema(description = "KYC 材料短期上传凭证")
public record KycMaterialUploadTicketVO(
        @Schema(description = "材料令牌：即对象存储键，提交实名认证时用于绑定材料",
                example = "KYC_ID_CARD/2026/08/22/3f2a9d8c_id-card-front.jpg")
        String materialToken,
        @Schema(description = "短期直传地址；直传通道接入前为空，客户端需经由服务端中转上传",
                nullable = true)
        String uploadUrl,
        @Schema(description = "凭证有效期（秒）", example = "300")
        Long expiresInSeconds
) {
}
