package interview.system.auth.vo;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

/**
 * @author zhuxi
 * @apiNote 登录响应
 * @since 2026/5/27 14:05
 */

@Builder
public record LoginVO (
        Long userId,
        String username,
        String nickname,
        String avatarUrl,
        String userType,
        String accessToken,
        String refreshToken,
        Long expiresIn
){}