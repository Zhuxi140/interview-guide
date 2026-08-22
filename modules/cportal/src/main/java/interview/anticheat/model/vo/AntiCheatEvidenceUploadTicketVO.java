package interview.anticheat.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 防作弊证据短期上传凭证。
 */
@Builder
@Schema(description = "防作弊证据短期上传凭证")
public record AntiCheatEvidenceUploadTicketVO(
        @Schema(description = "证据材料令牌：即对象存储键，批量上报事件时用于绑定证据",
                example = "ATTACHMENT/2026/08/22/3f2a9d8c_page-blur-001.jpg")
        String evidenceMaterialToken,
        @Schema(description = "短期直传地址；直传通道接入前为空，客户端需经由服务端中转上传",
                nullable = true)
        String uploadUrl,
        @Schema(description = "凭证有效期（秒）", example = "300")
        Long expiresInSeconds
) {
}
