package interview.system.auth.service;

import interview.api.system.dto.SecureChallengeStartDTO;
import interview.common.enums.SecureActionType;
import interview.system.auth.model.req.SecureChallengeVerifyReq;
import interview.system.auth.model.vo.SecureActionTokenVO;

/**
 * 安全验证 Challenge 服务。
 *
 * @author zhuxi
 */
public interface SecureChallengeService {

    /**
     * 创建安全验证 Challenge 并发送验证码
     * @param userId 用户 ID
     * @param actionType 安全操作类型
     * @param resourceId 业务资源 ID
     * @return Challenge 启动信息
     */
    SecureChallengeStartDTO create(
            Long userId, SecureActionType actionType, Long resourceId);

    /**
     * 核销 Challenge 验证码并签发一次性安全操作令牌
     * @param challengeId Challenge ID
     * @param req 验证码请求
     * @return 安全操作令牌
     */
    SecureActionTokenVO verify(String challengeId, SecureChallengeVerifyReq req);
}
