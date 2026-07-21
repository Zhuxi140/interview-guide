package interview.system.auth;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import interview.api.system.dto.SecureChallengeStartDTO;
import interview.common.constant.SecureActionContext;
import interview.common.enums.*;
import interview.common.exception.BusinessException;
import interview.common.util.CryptoUtil;
import interview.common.util.JwttUtil;
import interview.framework.config.properties.JwtProperties;
import interview.framework.context.AuthContext;
import interview.system.auth.model.bo.LoginBO;
import interview.system.auth.model.bo.RegisterBo;
import interview.system.auth.model.bo.UserInfoBO;
import interview.system.auth.model.entity.UserToken;
import interview.common.enums.SmsType;
import interview.system.auth.model.req.*;
import interview.system.auth.model.vo.RefreshTokenVO;
import interview.system.auth.model.vo.SwitchEnterpriseVO;
import interview.system.auth.model.vo.TokenInfoVO;
import interview.system.auth.service.impl.AuthServiceImpl;
import interview.system.auth.service.SecureChallengeService;
import interview.system.auth.service.SmsService;
import interview.system.auth.service.UsersService;
import interview.system.rbac.mapper.PermissionsMapper;
import interview.system.rbac.model.entity.Role;
import interview.system.auth.model.entity.User;
import interview.system.rbac.model.entity.UserRole;
import interview.system.auth.model.enums.UserStatus;
import interview.system.rbac.service.*;
import interview.system.tenant.service.EnterpriseTeamMembersService;
import interview.system.tenant.service.EnterprisesService;
import interview.system.tenant.model.entity.Enterprise;
import interview.system.tenant.model.entity.EnterpriseTeamMember;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.OffsetDateTime;
import java.util.*;

import static interview.system.TestMockUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private SmsService smsService;
    @Mock private UsersService usersService;
    @Mock private EnterprisesService enterprisesService;
    @Mock private UserRolesService userRolesService;
    @Mock private RolesService rolesService;
    @Mock private PermissionsMapper permissionsMapper;
    @Mock private EnterpriseTeamMembersService enterpriseTeamMembersService;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private JwttUtil jwttUtil;
    @Mock private JwtProperties jwtProperties;
    @Mock private SecureChallengeService secureChallengeService;
    @Mock private ValueOperations<String, String> valueOps;

    private AuthServiceImpl authService;
    private LambdaQueryChainWrapper<UserToken> tokenQueryWrapper;
    private LambdaUpdateChainWrapper<UserToken> tokenUpdateWrapper;

    private final Long userId = 1L;
    private final String username = "testuser";
    private final String password = "password123";
    private final String phone = "13812345678";
    private final String email = "test@example.com";
    private final String deviceInfo = "Mozilla/5.0";
    private final String ipAddress = "127.0.0.1";
    private final String accessToken = "jwt.token.string";
    private final String refreshToken = "uuid-refresh-token";

    @BeforeEach
    void setUp() {
        authService = spy(new AuthServiceImpl(
            smsService, usersService, enterprisesService, userRolesService,
            rolesService, permissionsMapper, enterpriseTeamMembersService,
            stringRedisTemplate, jwttUtil, jwtProperties, secureChallengeService
        ));
        tokenQueryWrapper = mockQueryWrapper();
        tokenUpdateWrapper = mockUpdateWrapper();
    }

    @AfterEach
    void tearDown() {
        AuthContext.remove();
    }

    // ============================== register ==============================

    @Nested
    class Register {

        private RegisterReq req;

        @BeforeEach
        void setUp() {
            req = new RegisterReq();
            req.setUsername(username);
            req.setPassword(password);
            req.setPhone(phone);
            req.setEmail(email);
            req.setCode("1234");
            req.setUserType("CANDIDATE");
        }

        @Test
        void register_success_candidate() {
            doNothing().when(smsService).verifyCode(phone, "1234", SmsType.REGISTER);
            when(jwtProperties.getExpiration()).thenReturn(30L);
            doReturn(true).when(authService).save(any(UserToken.class));

            LambdaQueryChainWrapper<User> q = mockQueryWrapper();
            when(q.exists()).thenReturn(false);
            when(usersService.lambdaQuery()).thenReturn(q);
            doAnswer(invocation -> {
                User u = invocation.getArgument(0);
                u.setId(userId);
                return true;
            }).when(usersService).save(any(User.class));
            when(jwttUtil.generatorToken(anyMap(), any())).thenReturn(accessToken);

            LambdaQueryChainWrapper<Role> roleCheckQ = mockQueryWrapper();
            when(roleCheckQ.exists()).thenReturn(true);
            when(rolesService.lambdaQuery()).thenReturn(roleCheckQ);

            RegisterBo result = authService.register(req);

            assertNotNull(result);
            assertEquals(userId, result.userId());
            assertEquals(username, result.username());
            assertNotNull(result.accessToken());
            assertNotNull(result.refreshToken());
            verify(smsService).verifyCode(phone, "1234", SmsType.REGISTER);
            verify(usersService).save(any(User.class));
            verify(userRolesService).save(any(UserRole.class));
        }

        @Test
        void register_fail_usernameDuplicated() {
            doNothing().when(smsService).verifyCode(phone, "1234", SmsType.REGISTER);

            LambdaQueryChainWrapper<User> q = mockQueryWrapper();
            when(q.exists()).thenReturn(true);
            when(usersService.lambdaQuery()).thenReturn(q);

            BusinessException ex = assertThrows(BusinessException.class, () -> authService.register(req));
            assertEquals(ErrorCode.USERNAME_ALREADY_EXISTS.getCode(), ex.getCode());
        }

        @Test
        void register_fail_phoneDuplicated() {
            doNothing().when(smsService).verifyCode(phone, "1234", SmsType.REGISTER);

            LambdaQueryChainWrapper<User> q = mockQueryWrapper();
            when(q.exists()).thenReturn(false, true);
            when(usersService.lambdaQuery()).thenReturn(q);

            BusinessException ex = assertThrows(BusinessException.class, () -> authService.register(req));
            assertEquals(ErrorCode.PHONE_ALREADY_EXISTS.getCode(), ex.getCode());
        }
    }

    // ============================== login ==============================

    @Nested
    class Login {

        private LoginReq req;

        @BeforeEach
        void setUp() {
            req = new LoginReq();
            req.setUsername(username);
            req.setPassword(password);
            req.setDeviceInfo(deviceInfo);
            req.setIpAddress(ipAddress);
        }

        @Test
        void login_success() {
            when(jwtProperties.getExpiration()).thenReturn(30L);

            try (MockedStatic<CryptoUtil> cryptoUtil = mockStatic(CryptoUtil.class)) {
                cryptoUtil.when(() -> CryptoUtil.checkPassword(eq(password), anyString())).thenReturn(true);

                mockCheckAndGetUser();
                prepareChainWrappers();
                mockEnterpriseContext_noEnterprise();
                mockRbacContext();

                LoginBO result = authService.login(req);

                assertNotNull(result);
                assertEquals(userId, result.userId());
                assertNotNull(result.accessToken());
                assertNotNull(result.refreshToken());
            }
        }

        @Test
        void login_fail_userNotFound() {
            mockCheckAndGetUserNotFound();
            BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(req));
            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        void login_fail_wrongPassword() {
            try (MockedStatic<CryptoUtil> cryptoUtil = mockStatic(CryptoUtil.class)) {
                cryptoUtil.when(() -> CryptoUtil.checkPassword(anyString(), anyString())).thenReturn(false);
                mockCheckAndGetUser();
                BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(req));
                assertEquals(ErrorCode.PASSWORD_ERROR.getCode(), ex.getCode());
            }
        }

        @Test
        void login_fail_userFrozen() {
            mockCheckAndGetUserFrozen();
            BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(req));
            assertEquals(ErrorCode.USER_ALREADY_FREEZE.getCode(), ex.getCode());
        }

        private void mockCheckAndGetUser() {
            LambdaQueryChainWrapper<User> q = mockQueryWrapper();
            User user = User.builder()
                .id(userId).username(username).phone(phone).email(email)
                .passwordHash("$2a$10$hashed").nickname("test")
                .userType(UserType.CANDIDATE).riskLevel(RiskLevel.NO_RISK)
                .status(UserStatus.NORMAL)
                .build();
            when(q.one()).thenReturn(user);
            when(usersService.lambdaQuery()).thenReturn(q);
        }

        private void mockCheckAndGetUserNotFound() {
            LambdaQueryChainWrapper<User> q = mockQueryWrapper();
            when(q.one()).thenReturn(null);
            when(usersService.lambdaQuery()).thenReturn(q);
        }

        private void mockCheckAndGetUserFrozen() {
            LambdaQueryChainWrapper<User> q = mockQueryWrapper();
            User user = User.builder()
                .id(userId).username(username)
                .passwordHash("$2a$10$hashed")
                .userType(UserType.CANDIDATE)
                .status(UserStatus.DISABLED)
                .riskLevel(RiskLevel.NO_RISK)
                .build();
            when(q.one()).thenReturn(user);
            when(usersService.lambdaQuery()).thenReturn(q);
        }

        @Test
        void login_success_withEnterprise() {
            when(jwtProperties.getExpiration()).thenReturn(30L);
            Long enterpriseId = 100L;

            try (MockedStatic<CryptoUtil> cryptoUtil = mockStatic(CryptoUtil.class)) {
                cryptoUtil.when(() -> CryptoUtil.checkPassword(eq(password), anyString())).thenReturn(true);

                mockCheckAndGetUser();
                prepareChainWrappers();
                mockEnterpriseContext_withEnterprise(enterpriseId);
                mockRbacContext();

                LoginBO result = authService.login(req);

                assertNotNull(result);
                assertEquals(userId, result.userId());
                assertEquals(enterpriseId, result.enterpriseId());
                assertNotNull(result.enterprises());
                assertEquals(1, result.enterprises().size());
                assertEquals("TestCorp", result.enterprises().get(0).name());
                assertNotNull(result.accessToken());
                assertNotNull(result.refreshToken());
            }
        }

        private void mockEnterpriseContext_withEnterprise(Long enterpriseId) {
            EnterpriseTeamMember member = EnterpriseTeamMember.builder()
                    .enterpriseId(enterpriseId)
                    .build();

            LambdaQueryChainWrapper<EnterpriseTeamMember> q = mockQueryWrapper();
            when(q.list()).thenReturn(List.of(member));
            when(enterpriseTeamMembersService.lambdaQuery()).thenReturn(q);

            Enterprise enterprise = Enterprise.builder()
                    .id(enterpriseId)
                    .name("TestCorp")
                    .shortName("TC")
                    .logoUrl("http://logo.url")
                    .build();

            LambdaQueryChainWrapper<Enterprise> eq = mockQueryWrapper();
            when(eq.list()).thenReturn(List.of(enterprise));
            when(enterprisesService.lambdaQuery()).thenReturn(eq);
        }

        private void prepareChainWrappers() {
            doReturn(tokenUpdateWrapper).when(authService).lambdaUpdate();
            doReturn(true).when(authService).save(any(UserToken.class));
            when(tokenUpdateWrapper.update()).thenReturn(true);
        }

        private void mockEnterpriseContext_noEnterprise() {
            LambdaQueryChainWrapper<EnterpriseTeamMember> q = mockQueryWrapper();
            when(q.list()).thenReturn(List.of());
            when(enterpriseTeamMembersService.lambdaQuery()).thenReturn(q);
        }

        private void mockRbacContext() {
            UserRole userRole = new UserRole();
            userRole.setRoleId(1);

            LambdaQueryChainWrapper<UserRole> urq = mockQueryWrapper();
            when(urq.list()).thenReturn(List.of(userRole));
            when(userRolesService.lambdaQuery()).thenReturn(urq);

            LambdaQueryChainWrapper<Role> rq = mockQueryWrapper();
            Role role1 = new Role();
            role1.setRoleCode("CANDIDATE");
            role1.setRoleScope(RoleScope.PLATFORM);
            when(rq.list()).thenReturn(List.of(role1));
            when(rolesService.lambdaQuery()).thenReturn(rq);

            when(permissionsMapper.getPermCodeByRoleId(anyList())).thenReturn(List.of("job:read", "job:write"));
            when(jwttUtil.generatorToken(anyMap(), eq(userId))).thenReturn(accessToken);
        }
    }

    // ============================== refreshToken ==============================

    @Nested
    class RefreshToken {

        private RefreshTokenReq req;

        @BeforeEach
        void setUp() {
            req = new RefreshTokenReq();
            req.setRefreshToken(refreshToken);
            req.setAccessToken(accessToken);
            doReturn(tokenQueryWrapper).when(authService).lambdaQuery();
        }

        @Test
        void refreshToken_success() {
            when(jwtProperties.getExpiration()).thenReturn(30L);

            UserToken token = UserToken.builder()
                .userId(userId).deviceInfo(deviceInfo).ipAddress(ipAddress).isRevoked(false)
                .build();

            when(tokenQueryWrapper.one()).thenReturn(token);
            doReturn(tokenUpdateWrapper).when(authService).lambdaUpdate();
            LambdaQueryChainWrapper<User> userCheckQ = mockQueryWrapper();
            when(userCheckQ.exists()).thenReturn(true);
            LambdaQueryChainWrapper<User> userFreshQ = mockQueryWrapper();
            when(userFreshQ.one()).thenReturn(User.builder()
                .username(username).userType(UserType.CANDIDATE).riskLevel(RiskLevel.NO_RISK).build());
            when(usersService.lambdaQuery()).thenReturn(userCheckQ, userFreshQ);
            when(tokenUpdateWrapper.update()).thenReturn(true);
            doReturn(true).when(authService).save(any(UserToken.class));

            LambdaQueryChainWrapper<UserRole> userRoleQ = mockQueryWrapper();
            UserRole userRole = UserRole.builder().roleId(1).build();
            when(userRoleQ.list()).thenReturn(List.of(userRole));
            when(userRolesService.lambdaQuery()).thenReturn(userRoleQ);

            LambdaQueryChainWrapper<EnterpriseTeamMember> memberQ = mockQueryWrapper();
            when(enterpriseTeamMembersService.lambdaQuery()).thenReturn(memberQ);

            LambdaQueryChainWrapper<Role> roleQ = mockQueryWrapper();
            Role role = new Role();
            role.setRoleCode("ROLE_USER");
            role.setRoleScope(RoleScope.PLATFORM);
            when(roleQ.list()).thenReturn(List.of(role));
            when(rolesService.lambdaQuery()).thenReturn(roleQ);

            when(permissionsMapper.getPermCodeByRoleId(anyList())).thenReturn(List.of());

            Claims claims = mock(Claims.class);
            when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 5000));
            when(jwttUtil.parseToken(accessToken)).thenReturn(claims);
            when(jwttUtil.generatorToken(anyMap(), eq(userId))).thenReturn("new-jwt-token");
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);

            RefreshTokenVO result = authService.refreshToken(req);

            assertNotNull(result);
            assertNotNull(result.accessToken());
            assertNotNull(result.refreshToken());
            verify(stringRedisTemplate).opsForValue();
        }

        @Test
        void refreshToken_fail_tokenNotFound() {
            when(tokenQueryWrapper.one()).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class, () -> authService.refreshToken(req));
            assertEquals(ErrorCode.ACCOUNT_DATA_ANOMALY.getCode(), ex.getCode());
        }

        @Test
        void refreshToken_fail_replayAttack() {
            UserToken revokedToken = UserToken.builder()
                .userId(userId).deviceInfo(deviceInfo).ipAddress(ipAddress).isRevoked(true)
                .build();

            when(tokenQueryWrapper.one()).thenReturn(revokedToken);
            doReturn(tokenUpdateWrapper).when(authService).lambdaUpdate();
            LambdaQueryChainWrapper<User> userCheckQ = mockQueryWrapper();
            when(userCheckQ.exists()).thenReturn(true);
            when(usersService.lambdaQuery()).thenReturn(userCheckQ);

            BusinessException ex = assertThrows(BusinessException.class, () -> authService.refreshToken(req));
            assertEquals(ErrorCode.RISK_CONTROL.getCode(), ex.getCode());
        }
    }

    // ============================== logout ==============================

    @Nested
    class Logout {

        private LogoutReq req;

        @BeforeEach
        void setUp() {
            AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(userId).build());
            req = new LogoutReq();
            req.setAccessToken(accessToken);
            req.setRefreshToken(refreshToken);
            doReturn(tokenUpdateWrapper).when(authService).lambdaUpdate();
        }

        @Test
        void logout_success() {
            Claims claims = mock(Claims.class);
            when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 5000));
            when(jwttUtil.parseToken(accessToken)).thenReturn(claims);
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);

            authService.logout(req);

            verify(valueOps).set(anyString(), anyString(), anyLong(), any());
            verify(tokenUpdateWrapper).update();
        }
    }

    // ============================== getUserToken ==============================

    @Nested
    class GetUserToken {

        @BeforeEach
        void setUp() {
            doReturn(tokenQueryWrapper).when(authService).lambdaQuery();
        }

        @Test
        void getUserToken_success() {
            AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(userId).userType(UserType.HR).build());

            UserToken token = UserToken.builder()
                .id(1L).deviceInfo(deviceInfo).ipAddress(ipAddress)
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .createdAt(OffsetDateTime.now())
                .build();

            when(tokenQueryWrapper.list()).thenReturn(List.of(token));

            List<TokenInfoVO> result = authService.getUserToken();

            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            assertEquals(1L, result.get(0).tokenId());
            assertEquals(deviceInfo, result.get(0).deviceInfo());
        }
    }

    // ============================== revoke ==============================

    @Nested
    class Revoke {

        private SecureActionContext secureActionContext;

        @BeforeEach
        void setUp() {
            AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(userId).userType(UserType.HR).build());
            secureActionContext = SecureActionContext.builder()
                    .actionType(SecureActionType.REVOKE_DEVICE)
                    .resourceId(1L)
                    .build();
        }

        @Test
        void startRevokeChallenge_success() {
            doReturn(tokenQueryWrapper).when(authService).lambdaQuery();
            when(tokenQueryWrapper.exists()).thenReturn(true);
            when(secureChallengeService.create(
                    userId, SecureActionType.REVOKE_DEVICE, 1L))
                    .thenReturn(new SecureChallengeStartDTO(
                            "challenge-1", "138****5678", 300));

            assertEquals("challenge-1",
                    authService.startRevokeChallenge(1L).challengeId());
        }

        @Test
        void revoke_success() {
            doReturn(tokenUpdateWrapper).when(authService).lambdaUpdate();
            when(tokenUpdateWrapper.update()).thenReturn(true);

            assertDoesNotThrow(() -> authService.revoke(1L, secureActionContext));
        }

        @Test
        void revoke_fail_tokenNotFound() {
            doReturn(tokenUpdateWrapper).when(authService).lambdaUpdate();
            when(tokenUpdateWrapper.update()).thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.revoke(1L, secureActionContext));
            assertEquals(ErrorCode.ACCOUNT_DATA_ANOMALY.getCode(), ex.getCode());
        }
    }

    // ============================== switchEnterprise ==============================

    @Nested
    class SwitchEnterprise {

        private final Long targetEnterpriseId = 200L;
        private final String oldAccessToken = "old-jwt-token";

        @BeforeEach
        void setUp() {
            Map<Long, List<interview.common.enums.Role>> entRoleMap = new HashMap<>();
            entRoleMap.put(targetEnterpriseId, List.of(interview.common.enums.Role.ENTERPRISE_OWNER));

            AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                    .userId(userId)
                    .username(username)
                    .userType(UserType.HR)
                    .riskLevel(RiskLevel.NO_RISK)
                    .platformRoleCodes(List.of())
                    .entRoleMap(entRoleMap)
                    .build());
        }

        @Test
        void switchEnterprise_success() {
            Claims claims = mock(Claims.class);
            when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 5000));
            when(jwttUtil.parseToken(oldAccessToken)).thenReturn(claims);
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
            when(jwtProperties.getExpiration()).thenReturn(30L);
            when(jwttUtil.generatorToken(anyMap(), eq(userId))).thenReturn("new-jwt-token");

            SwitchEnterpriseVO result = authService.switchEnterprise(targetEnterpriseId, oldAccessToken);

            assertNotNull(result);
            assertEquals("new-jwt-token", result.accessToken());
            assertEquals(1800L, result.expiresIn());
            verify(valueOps).set(anyString(), anyString(), anyLong(), any());
        }

        @Test
        void switchEnterprise_fail_notBelong() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.switchEnterprise(999L, oldAccessToken));
            assertEquals(ErrorCode.ENTERPRISE_NOT_BELONG.getCode(), ex.getCode());
        }
    }

    // ============================== getUserInfo ==============================

    @Nested
    class GetUserInfo {

        @BeforeEach
        void setUp() {
            AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(userId).userType(UserType.CANDIDATE).build());
        }

        @Test
        void getUserInfo_success() {
            User user = User.builder()
                .username(username).nickname("testuser").phone(phone).email(email)
                .avatarUrl("http://avatar.url").status(UserStatus.NORMAL)
                .build();

            LambdaQueryChainWrapper<User> q = mockQueryWrapper();
            when(q.one()).thenReturn(user);
            when(usersService.lambdaQuery()).thenReturn(q);

            UserInfoBO result = authService.getUserInfo();

            assertNotNull(result);
            assertEquals(username, result.username());
            assertEquals(phone, result.phone());
            assertEquals(UserStatus.NORMAL, result.status());
        }

        @Test
        void getUserInfo_fail_userNotFound() {
            LambdaQueryChainWrapper<User> q = mockQueryWrapper();
            when(q.one()).thenReturn(null);
            when(usersService.lambdaQuery()).thenReturn(q);

            BusinessException ex = assertThrows(BusinessException.class, () -> authService.getUserInfo());
            assertEquals(ErrorCode.ACCOUNT_DATA_ANOMALY.getCode(), ex.getCode());
        }

        @Test
        void getUserInfo_fail_userFrozen() {
            User user = User.builder().status(UserStatus.DISABLED).build();

            LambdaQueryChainWrapper<User> q = mockQueryWrapper();
            when(q.one()).thenReturn(user);
            when(usersService.lambdaQuery()).thenReturn(q);

            BusinessException ex = assertThrows(BusinessException.class, () -> authService.getUserInfo());
            assertEquals(ErrorCode.USER_ALREADY_FREEZE.getCode(), ex.getCode());
        }
    }
}
