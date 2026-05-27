package interview.system.auth.vo;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * @author zhuxi
 * @apiNote 活跃设备列表项响应
 * @since 2026/5/27 14:05
 */
@Builder
public record TokenInfoVO (
        Long tokenId,
        String deviceInfo,
        String ipAddress,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
){}

