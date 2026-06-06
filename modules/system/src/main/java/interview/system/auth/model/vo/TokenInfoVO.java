package interview.system.auth.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 活跃设备列表项响应
 * @since 2026/5/27 14:05
 */
@Builder
@Schema(description = "活跃设备列表项响应")
public record TokenInfoVO (

    @Schema(description = "Token 记录 ID")
    Long tokenId,

    @Schema(description = "设备 UA")
    String deviceInfo,

    @Schema(description = "登录 IP")
    String ipAddress,

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    OffsetDateTime createdAt,

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "过期时间")
    OffsetDateTime expiresAt
) {}
