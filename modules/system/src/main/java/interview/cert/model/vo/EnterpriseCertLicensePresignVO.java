package interview.cert.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 营业执照短期审核（下载）地址。
 */
@Builder
@Schema(description = "营业执照短期审核地址")
public record EnterpriseCertLicensePresignVO(
        @Schema(description = "短期预签名下载地址")
        String downloadUrl,
        @Schema(description = "地址有效期（秒）", example = "300")
        Long expiresInSeconds
) {
}
