package interview.kyc.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * KYC 材料短期审核（下载）地址。
 */
@Builder
@Schema(description = "KYC 材料短期审核地址")
public record KycMaterialPresignVO(
        @Schema(description = "短期预签名下载地址")
        String downloadUrl,
        @Schema(description = "地址有效期（秒）", example = "300")
        Long expiresInSeconds
) {
}
