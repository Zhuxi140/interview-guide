package interview.system.auth.model.bo;


import lombok.Builder;

import java.util.List;

@Builder
public record LoginBO(
        Long userId,
        Long enterpriseId,
        String enterpriseName,
        String logoUrl,
        String username,
        String nickname,
        String avatarUrl,
        String userType,
        List<String> roles,
        List<String> permissions,
        String accessToken,
        String refreshToken,
        Long expiresIn
) { }
