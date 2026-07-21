package interview.api.system;

import interview.api.system.dto.SecureChallengeStartDTO;
import interview.common.enums.SecureActionType;

/**
 * 安全验证 Challenge 内部 API，由认证模块实现并供业务模块调用。
 *
 * @author zhuxi
 */
public interface SecureChallengeApi {

    /**
     * 为指定用户、动作和业务资源创建一次安全验证 Challenge
     * @param userId 用户 ID
     * @param actionType 安全操作类型
     * @param resourceId 业务资源 ID
     * @return Challenge 启动信息
     */
    SecureChallengeStartDTO create(
            Long userId, SecureActionType actionType, Long resourceId);
}
