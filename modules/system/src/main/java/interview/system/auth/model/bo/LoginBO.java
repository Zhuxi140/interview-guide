package interview.system.auth.model.bo;

import interview.common.enums.UserType;
import interview.system.auth.model.vo.UserEnterpriseVO;
import lombok.Builder;

import java.util.List;

@Builder
public record LoginBO(
        Long userId,
        Long enterpriseId,
        String enterpriseName,
        String logoUrl,
        List<UserEnterpriseVO> enterprises,
        String username,
        String nickname,
        String avatarUrl,
        UserType userType,
        List<String> roles,
        List<String> permissions,
        String accessToken,
        String refreshToken,
        Long expiresInSeconds
) { }
