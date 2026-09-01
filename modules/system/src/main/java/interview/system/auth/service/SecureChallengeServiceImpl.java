package interview.system.auth.service;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import interview.api.system.dto.SecureChallengeStartDTO;
import interview.common.constant.AuthKeyConstant;
import interview.common.constant.SecureActionContext;
import interview.common.enums.ErrorCode;
import interview.common.enums.SecureActionType;
import interview.common.enums.SmsType;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.system.auth.model.entity.User;
import interview.system.auth.model.enums.SecureChallengeStatus;
import interview.system.auth.model.req.SecureChallengeVerifyReq;
import interview.system.auth.model.vo.SecureActionTokenVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 安全验证 Challenge 服务实现，负责维护 Redis 状态并签发一次性令牌。
 *
 * @author zhuxi
 */
@Service
@RequiredArgsConstructor
public class SecureChallengeServiceImpl implements SecureChallengeService {

    private static final long CHALLENGE_TTL_MINUTES = 10;
    private static final long CODE_TTL_SECONDS = 300;
    private static final long SECURE_TOKEN_TTL_SECONDS = 300;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final DefaultRedisScript<String> VERIFY_CHALLENGE_SCRIPT;

    // 加载 Challenge 验证码核销与令牌签发的 Redis Lua 脚本
    static {
        VERIFY_CHALLENGE_SCRIPT = new DefaultRedisScript<>();
        VERIFY_CHALLENGE_SCRIPT.setScriptText(
                ResourceUtil.readUtf8Str("SecureChallengeVerify.lua"));
        VERIFY_CHALLENGE_SCRIPT.setResultType(String.class);
    }

    private final StringRedisTemplate stringRedisTemplate;
    private final UsersService usersService;
    private final SmsService smsService;

    @Override
    public SecureChallengeStartDTO create(
            Long userId, SecureActionType actionType, Long resourceId) {
        // Challenge 只能由当前登录用户为明确的业务动作和资源创建
        if (userId == null || actionType == null || resourceId == null) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR);
        }
        if (!userId.equals(AuthContext.getRequiredUserId())) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }

        // 验证手机号始终由服务端读取，禁止客户端决定验证码接收号码
        User user = usersService.lambdaQuery()
                .select(User::getId, User::getPhone)
                .eq(User::getId, userId)
                .one();
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (StrUtil.isBlank(user.getPhone())) {
            throw new BusinessException(ErrorCode.NOT_YOUR_PHONE);
        }

        // 保存用户、动作、业务资源和验证手机号组成的 Challenge 上下文
        String challengeId = IdUtil.fastSimpleUUID();
        String challengeKey = AuthKeyConstant.getSecureChallengeKey(challengeId);
        String codeKey = AuthKeyConstant.getSecureChallengeCodeKey(challengeId);
        Map<String, String> challenge = new HashMap<>();
        challenge.put(AuthKeyConstant.SECURE_CHALLENGE_FIELD_USER_ID, userId.toString());
        challenge.put(AuthKeyConstant.SECURE_CHALLENGE_FIELD_ACTION_TYPE, actionType.name());
        challenge.put(AuthKeyConstant.SECURE_CHALLENGE_FIELD_RESOURCE_ID, resourceId.toString());
        challenge.put(AuthKeyConstant.SECURE_CHALLENGE_FIELD_VERIFY_PHONE, user.getPhone());
        challenge.put(AuthKeyConstant.SECURE_CHALLENGE_FIELD_STATUS,
                SecureChallengeStatus.WAIT_VERIFY.name());
        challenge.put(AuthKeyConstant.SECURE_CHALLENGE_FIELD_FAILED_ATTEMPTS, "0");
        challenge.put(AuthKeyConstant.SECURE_CHALLENGE_FIELD_SECURE_TOKEN, "");
        stringRedisTemplate.opsForHash().putAll(challengeKey, challenge);
        stringRedisTemplate.expire(challengeKey, CHALLENGE_TTL_MINUTES, TimeUnit.MINUTES);

        // 发送失败时删除已创建的 Challenge 和验证码，避免残留无效状态
        try {
            smsService.sendCode(user.getPhone(), SmsType.SECURE_CHALLENGE, codeKey);
        } catch (Exception e) {
            stringRedisTemplate.delete(List.of(challengeKey, codeKey));
            throw e;
        }

        return new SecureChallengeStartDTO(
                challengeId,
                StrUtil.hide(user.getPhone(), 3, 7),
                CODE_TTL_SECONDS);
    }

    @Override
    public SecureActionTokenVO verify(
            String challengeId, SecureChallengeVerifyReq req) {
        Long userId = AuthContext.getRequiredUserId();

        // 读取 Challenge 并校验其属于当前登录用户
        Map<String, String> challenge = getChallenge(challengeId);
        if (!userId.toString().equals(
                challenge.get(AuthKeyConstant.SECURE_CHALLENGE_FIELD_USER_ID))) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }

        // 已验证请求保持幂等，仅返回仍未消费且未过期的原令牌
        String status = challenge.get(AuthKeyConstant.SECURE_CHALLENGE_FIELD_STATUS);
        if (SecureChallengeStatus.VERIFIED.name().equals(status)) {
            return getExistingToken(challenge);
        }
        if (!SecureChallengeStatus.WAIT_VERIFY.name().equals(status)) {
            throw new BusinessException(ErrorCode.SECURE_CHALLENGE_STATE_INVALID);
        }

        // 从服务端保存的 Challenge 构造业务动作上下文
        SecureActionType actionType;
        Long resourceId;
        try {
            actionType = SecureActionType.valueOf(
                    challenge.get(AuthKeyConstant.SECURE_CHALLENGE_FIELD_ACTION_TYPE));
            resourceId = Long.valueOf(
                    challenge.get(AuthKeyConstant.SECURE_CHALLENGE_FIELD_RESOURCE_ID));
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.SECURE_CHALLENGE_INVALID);
        }

        String secureToken = challengeId + "." + IdUtil.fastSimpleUUID();
        SecureActionContext context = SecureActionContext.builder()
                .userId(userId)
                .actionType(actionType)
                .resourceId(resourceId)
                .enterpriseId(isEnterpriseAction(actionType) ? resourceId : null)
                .challengeId(challengeId)
                .build();

        // 原子核销验证码、累计失败次数、推进状态并写入一次性令牌
        String result = stringRedisTemplate.execute(
                VERIFY_CHALLENGE_SCRIPT,
                List.of(
                        AuthKeyConstant.getSecureChallengeKey(challengeId),
                        AuthKeyConstant.getSecureChallengeCodeKey(challengeId),
                        AuthKeyConstant.getSecureActionTokenKey(secureToken)),
                userId.toString(),
                SecureChallengeStatus.WAIT_VERIFY.name(),
                SecureChallengeStatus.VERIFIED.name(),
                req.getCode(),
                secureToken,
                JSONUtil.toJsonStr(context),
                Long.toString(SECURE_TOKEN_TTL_SECONDS),
                Integer.toString(MAX_FAILED_ATTEMPTS));

        return handleVerifyResult(result);
    }

    private boolean isEnterpriseAction(SecureActionType actionType) {
        // 企业动作同时保留明确的 enterpriseId，便于业务服务执行防御性校验
        return actionType == SecureActionType.UPDATE_ENTERPRISE_EMAIL
                || actionType == SecureActionType.UPDATE_ENTERPRISE_PHONE
                || actionType == SecureActionType.DELETE_ENTERPRISE;
    }

    private Map<String, String> getChallenge(String challengeId) {
        // 空 ID 或空 Hash 均表示 Challenge 不存在或已经过期
        if (StrUtil.isBlank(challengeId)) {
            throw new BusinessException(ErrorCode.SECURE_CHALLENGE_INVALID);
        }
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(
                AuthKeyConstant.getSecureChallengeKey(challengeId));
        if (entries.isEmpty()) {
            throw new BusinessException(ErrorCode.SECURE_CHALLENGE_INVALID);
        }

        Map<String, String> challenge = new HashMap<>();
        entries.forEach((key, value) -> challenge.put(key.toString(), value.toString()));
        return challenge;
    }

    private SecureActionTokenVO getExistingToken(Map<String, String> challenge) {
        // 已签发令牌必须仍存在；已经消费后不得通过重复验证重新签发
        String token = challenge.get(AuthKeyConstant.SECURE_CHALLENGE_FIELD_SECURE_TOKEN);
        if (StrUtil.isBlank(token)
                || !Boolean.TRUE.equals(stringRedisTemplate.hasKey(
                AuthKeyConstant.getSecureActionTokenKey(token)))) {
            throw new BusinessException(ErrorCode.SECURE_CHALLENGE_STATE_INVALID);
        }
        return new SecureActionTokenVO(token, SECURE_TOKEN_TTL_SECONDS);
    }

    private SecureActionTokenVO handleVerifyResult(String result) {
        // 将 Lua 返回协议转换为统一的业务异常或令牌结果
        if (StrUtil.isBlank(result) || result.startsWith("0:")) {
            throw new BusinessException(ErrorCode.SECURE_CHALLENGE_INVALID);
        }
        if (result.startsWith("1:")) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
        if (result.startsWith("2:")) {
            throw new BusinessException(ErrorCode.SECURE_CHALLENGE_STATE_INVALID);
        }
        if (result.startsWith("3:")) {
            throw new BusinessException(ErrorCode.CODE_EXPIRED);
        }
        if (result.startsWith("4:")) {
            throw new BusinessException(ErrorCode.CODE_ERROR);
        }
        if (result.startsWith("5:")) {
            throw new BusinessException(ErrorCode.SECURE_CHALLENGE_ATTEMPTS_EXCEEDED);
        }
        if (!result.startsWith("6:") && !result.startsWith("7:")) {
            throw new BusinessException(ErrorCode.SECURE_CHALLENGE_INVALID);
        }

        String token = result.substring(result.indexOf(':') + 1);
        if (StrUtil.isBlank(token)
                || !Boolean.TRUE.equals(stringRedisTemplate.hasKey(
                AuthKeyConstant.getSecureActionTokenKey(token)))) {
            throw new BusinessException(ErrorCode.SECURE_CHALLENGE_STATE_INVALID);
        }
        return new SecureActionTokenVO(token, SECURE_TOKEN_TTL_SECONDS);
    }
}
