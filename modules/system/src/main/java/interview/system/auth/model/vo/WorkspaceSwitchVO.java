package interview.system.auth.model.vo;

import interview.system.auth.model.enums.WorkspaceType;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author zhuxi
 * @apiNote 工作区切换响应
 */
@Schema(description = "工作区切换响应")
public record WorkspaceSwitchVO(
        @Schema(description = "当前工作区类型", example = "ENTERPRISE")
        WorkspaceType workspaceType,
        @Schema(description = "当前企业 ID；平台工作区为空")
        Long enterpriseId,
        @Schema(description = "包含新工作区上下文的短时 JWT")
        String accessToken,
        @Schema(description = "过期时间（秒）")
        Long expiresInSeconds
) {
}
