package interview.system.auth;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import interview.common.constant.AuthKeyConstant;
import interview.common.enums.ErrorCode;
import interview.common.enums.SecureActionType;
import interview.common.enums.SmsType;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.system.auth.model.entity.User;
import interview.system.auth.model.enums.SecureChallengeStatus;
import interview.system.auth.model.req.SecureChallengeVerifyReq;
import interview.system.auth.model.vo.SecureActionTokenVO;
import interview.system.auth.service.SmsService;
import interview.system.auth.service.UsersService;
import interview.system.auth.service.impl.SecureChallengeServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.Map;

import static interview.system.TestMockUtils.mockQueryWrapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 安全验证 Challenge 服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class SecureChallengeServiceImplTest {

    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private HashOperations<String, Object, Object> hashOperations;
    @Mock private UsersService usersService;
    @Mock private SmsService smsService;

    private SecureChallengeServiceImpl service;
    private final Long userId = 1L;
    private final Long enterpriseId = 100L;

    @BeforeEach
    void setUp() {
        service = new SecureChallengeServiceImpl(
                stringRedisTemplate, usersService, smsService);
        AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(userId)
                .build());
    }

    @AfterEach
    void tearDown() {
        AuthContext.remove();
    }

    @Test
    void create_shouldBindServerSidePhoneActionAndResource() {
        LambdaQueryChainWrapper<User> query = mockQueryWrapper();
        when(usersService.lambdaQuery()).thenReturn(query);
        when(query.one()).thenReturn(User.builder()
                .id(userId)
                .phone("13800138000")
                .build());
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);

        var result = service.create(
                userId,
                SecureActionType.UPDATE_ENTERPRISE_EMAIL,
                enterpriseId);

        assertNotNull(result.challengeId());
        assertEquals("138****8000", result.maskedPhone());
        verify(hashOperations).putAll(anyString(), argThat(challenge ->
                SecureActionType.UPDATE_ENTERPRISE_EMAIL.name().equals(
                        challenge.get(AuthKeyConstant.SECURE_CHALLENGE_FIELD_ACTION_TYPE))
                        && enterpriseId.toString().equals(
                        challenge.get(AuthKeyConstant.SECURE_CHALLENGE_FIELD_RESOURCE_ID))));
        verify(smsService).sendCode(
                eq("13800138000"),
                eq(SmsType.SECURE_CHALLENGE),
                contains(result.challengeId()));
    }

    @Test
    void create_shouldRejectDifferentLoggedInUser() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.create(
                        2L,
                        SecureActionType.DELETE_ENTERPRISE,
                        enterpriseId));

        assertEquals(ErrorCode.PERMISSION_DENIED.getCode(), exception.getCode());
        verifyNoInteractions(usersService, smsService);
    }

    @Test
    void verify_shouldIssueActionAndResourceBoundToken() {
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(anyString())).thenReturn(waitingChallenge());
        doReturn("6:challenge-1.token-1").when(stringRedisTemplate).execute(
                any(), anyList(),
                anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString());
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(true);
        SecureChallengeVerifyReq req = new SecureChallengeVerifyReq();
        req.setCode("123456");

        SecureActionTokenVO result = service.verify("challenge-1", req);

        assertEquals("challenge-1.token-1", result.secureActionToken());
        assertEquals(300, result.expiresInSeconds());
    }

    @Test
    void verify_shouldRejectChallengeOwnedByDifferentUser() {
        Map<Object, Object> challenge = waitingChallenge();
        challenge.put(AuthKeyConstant.SECURE_CHALLENGE_FIELD_USER_ID, "2");
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(anyString())).thenReturn(challenge);
        SecureChallengeVerifyReq req = new SecureChallengeVerifyReq();
        req.setCode("123456");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.verify("challenge-1", req));

        assertEquals(ErrorCode.PERMISSION_DENIED.getCode(), exception.getCode());
    }

    @Test
    void verify_shouldMapFifthFailureToAttemptsExceeded() {
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(anyString())).thenReturn(waitingChallenge());
        doReturn("5:").when(stringRedisTemplate).execute(
                any(), anyList(),
                anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString());
        SecureChallengeVerifyReq req = new SecureChallengeVerifyReq();
        req.setCode("000000");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.verify("challenge-1", req));

        assertEquals(
                ErrorCode.SECURE_CHALLENGE_ATTEMPTS_EXCEEDED.getCode(),
                exception.getCode());
    }

    @Test
    void verify_shouldReturnExistingUnconsumedToken() {
        Map<Object, Object> challenge = waitingChallenge();
        challenge.put(
                AuthKeyConstant.SECURE_CHALLENGE_FIELD_STATUS,
                SecureChallengeStatus.VERIFIED.name());
        challenge.put(
                AuthKeyConstant.SECURE_CHALLENGE_FIELD_SECURE_TOKEN,
                "challenge-1.token-1");
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(anyString())).thenReturn(challenge);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(true);
        SecureChallengeVerifyReq req = new SecureChallengeVerifyReq();
        req.setCode("123456");

        SecureActionTokenVO result = service.verify("challenge-1", req);

        assertEquals("challenge-1.token-1", result.secureActionToken());
    }

    private Map<Object, Object> waitingChallenge() {
        Map<Object, Object> challenge = new HashMap<>();
        challenge.put(AuthKeyConstant.SECURE_CHALLENGE_FIELD_USER_ID, userId.toString());
        challenge.put(
                AuthKeyConstant.SECURE_CHALLENGE_FIELD_ACTION_TYPE,
                SecureActionType.UPDATE_ENTERPRISE_EMAIL.name());
        challenge.put(
                AuthKeyConstant.SECURE_CHALLENGE_FIELD_RESOURCE_ID,
                enterpriseId.toString());
        challenge.put(
                AuthKeyConstant.SECURE_CHALLENGE_FIELD_VERIFY_PHONE,
                "13800138000");
        challenge.put(
                AuthKeyConstant.SECURE_CHALLENGE_FIELD_STATUS,
                SecureChallengeStatus.WAIT_VERIFY.name());
        challenge.put(AuthKeyConstant.SECURE_CHALLENGE_FIELD_FAILED_ATTEMPTS, "0");
        challenge.put(AuthKeyConstant.SECURE_CHALLENGE_FIELD_SECURE_TOKEN, "");
        return challenge;
    }
}
