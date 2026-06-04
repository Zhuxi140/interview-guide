package interview.system.auth.model.bo;

import lombok.Builder;

@Builder
public record RegisterBo(
        Long userId,
        String username,
        String accessToken,
        String refreshToken,
        Long expiresIn
) {
}
