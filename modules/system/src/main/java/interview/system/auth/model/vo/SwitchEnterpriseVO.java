package interview.system.auth.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "切换企业响应")
public record SwitchEnterpriseVO(
    @Schema(description = "新短时 JWT")
    String accessToken,
    @Schema(description = "过期时间（秒）")
    Long expiresIn
) {}
