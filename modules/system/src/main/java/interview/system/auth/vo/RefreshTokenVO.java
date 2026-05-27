package interview.system.auth.vo;

import lombok.Builder;

/**
 * @author zhuxi
 * @apiNote 刷新 Token 响应
 * @since 2026/5/27 14:05
 */

@Builder
public record RefreshTokenVO (

    String accessToken,
    String refreshToken,
    Long expiresIn
){}
