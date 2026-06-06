package interview.system.auth.model.bo;

import interview.system.rbac.model.enums.UserStatus;
import lombok.Builder;

@Builder
public record UserInfoBO(
        Long id,
        String username,
        String email,
        String nickname,
        String avatarUrl,
        String phone,
        String userType,
        UserStatus status
) {
}
