package interview.anticheat.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 防作弊证据短期下载地址。
 */
@Builder
@Schema(description = "防作弊证据短期下载地址")
public record AntiCheatEvidencePresignVO(
        @Schema(description = "短期预签名下载地址")
        String downloadUrl,
        @Schema(description = "地址有效期（秒）", example = "300")
        Long expiresInSeconds
) {
}
