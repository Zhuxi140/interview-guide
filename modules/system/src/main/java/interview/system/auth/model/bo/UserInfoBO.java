package interview.system.auth.model.bo;

import interview.system.auth.model.enums.UserStatus;
import interview.system.auth.model.vo.UserEnterpriseVO;
import lombok.Builder;

import java.util.List;

@Builder
public record UserInfoBO(
        Long id,
        String username,
        String email,
        String nickname,
        String avatarUrl,
        String phone,
        String userType,
        Long enterpriseId,
        String enterpriseName,
        String logoUrl,
        List<UserEnterpriseVO> enterprises,
        UserStatus status,
        List<String> roles,
        List<String> permissions
) {
}
