package interview.system.auth.api;

import interview.api.system.SecureChallengeApi;
import interview.api.system.dto.SecureChallengeStartDTO;
import interview.common.enums.SecureActionType;
import interview.system.auth.service.SecureChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 安全验证 Challenge 内部 API 实现。
 *
 * @author zhuxi
 */
@Service
@RequiredArgsConstructor
public class SecureChallengeApiImpl implements SecureChallengeApi {

    private final SecureChallengeService secureChallengeService;

    @Override
    public SecureChallengeStartDTO create(
            Long userId, SecureActionType actionType, Long resourceId) {
        // 统一委托认证模块创建并保存 Challenge
        return secureChallengeService.create(userId, actionType, resourceId);
    }
}
