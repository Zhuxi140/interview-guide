package interview.system.auth.vo;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

/**
 * @author zhuxi
 * @apiNote 当前用户信息响应
 * @since 2026/5/27 14:05
 */

@Builder
public record UserInfoVO (
        Long id,
        String username,
        String email,
        String nickname,
        String avatarUrl,
        String phone,
        String userType,
        Integer status
){}