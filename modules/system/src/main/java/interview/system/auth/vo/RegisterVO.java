package interview.system.auth.vo;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

/**
 * @author zhuxi
 * @apiNote 注册响应
 * @since 2026/5/27 14:05
 */

@Builder
public record RegisterVO (
        Long userId,
        String username,
        String accessToken,
        String refreshToken,
        Long expiresIn
){}

