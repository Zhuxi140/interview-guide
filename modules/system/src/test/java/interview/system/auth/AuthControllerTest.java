package interview.system.auth;

import interview.common.constant.Result;
import interview.common.constant.SecureActionContext;
import interview.common.enums.SmsType;
import interview.system.auth.model.bo.LoginBO;
import interview.system.auth.model.bo.RegisterBo;
import interview.system.auth.model.bo.UserInfoBO;
import interview.system.auth.model.enums.WorkspaceType;
import interview.system.auth.model.enums.UserStatus;
import interview.system.auth.model.req.*;
import interview.system.auth.model.vo.*;
import interview.system.auth.service.AuthService;
import interview.system.auth.service.SecureChallengeService;
import interview.system.auth.service.SmsService;
import interview.system.tenant.model.enums.EnterpriseStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private SmsService smsService;
    @Mock
    private SecureChallengeService secureChallengeService;
    @Mock
    private AuthService authService;
    @Mock
    private AuthConverter authConverter;
    @Mock
    private HttpServletRequest request;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(
                smsService, secureChallengeService, authService, authConverter, request);
    }

    @Nested
    class SendSms {

        @Test
        void sendSms_success() {
            SmsSendReq req = new SmsSendReq();
            req.setPhone("13800138000");
            req.setSmsType(SmsType.LOGIN);

            Result<?> result = authController.sendSms(req);

            assertNotNull(result);
            assertNotNull(result.getCode());
            verify(smsService).sendSms(req);
        }
    }

    @Nested
    class VerifyChallenge {

        @Test
        void verifyChallenge_success() {
            SecureChallengeVerifyReq req = new SecureChallengeVerifyReq();
            req.setCode("123456");
            SecureActionTokenVO vo = new SecureActionTokenVO("token-xxx", 300);
            when(secureChallengeService.verify("challenge-1", req)).thenReturn(vo);

            Result<SecureActionTokenVO> result = authController.verifySecureChallenge(
                    "challenge-1", req);

            assertEquals("token-xxx", result.getData().secureActionToken());
            verify(secureChallengeService).verify("challenge-1", req);
        }
    }

    @Nested
    class Register {

        private final RegisterReq req = new RegisterReq();
        private final RegisterBo bo = RegisterBo.builder()
                .userId(1L).username("testuser").accessToken("jwt-xxx")
                .refreshToken("rt-xxx").expiresInSeconds(1800L).build();
        private final RegisterVO vo = RegisterVO.builder()
                .userId(1L).username("testuser").accessToken("jwt-xxx")
                .refreshToken("rt-xxx").expiresInSeconds(1800L).build();

        @BeforeEach
        void setUp() {
            req.setUsername("testuser");
            req.setPassword("pass123");
            req.setPhone("13800138000");
            req.setCode("123456");
            req.setUserType("CANDIDATE");
        }

        @Test
        void register_success() {
            when(authService.register(req)).thenReturn(bo);
            when(authConverter.toRegisterVO(bo)).thenReturn(vo);

            Result<RegisterVO> result = authController.register(req);

            assertNotNull(result.getData());
            assertEquals(1L, result.getData().userId());
            assertEquals("jwt-xxx", result.getData().accessToken());
            verify(authService).register(req);
            verify(authConverter).toRegisterVO(bo);
        }
    }

    @Nested
    class Login {

        private final LoginReq req = new LoginReq();
        private final LoginBO bo = LoginBO.builder()
                .userId(1L).username("testuser").accessToken("jwt-xxx")
                .refreshToken("rt-xxx").expiresInSeconds(1800L).build();
        private final LoginVO vo = LoginVO.builder()
                .userId(1L).username("testuser").accessToken("jwt-xxx")
                .refreshToken("rt-xxx").expiresInSeconds(1800L).build();

        @BeforeEach
        void setUp() {
            req.setUsername("testuser");
            req.setPassword("pass123");
        }

        @Test
        void login_success() {
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(authService.login(req, "127.0.0.1")).thenReturn(bo);
            when(authConverter.toLoginVO(bo)).thenReturn(vo);

            Result<LoginVO> result = authController.login(req);

            assertNotNull(result.getData());
            assertEquals("jwt-xxx", result.getData().accessToken());
            verify(authService).login(req, "127.0.0.1");
            verify(authConverter).toLoginVO(bo);
        }
    }

    @Nested
    class Logout {

        @Test
        void logout_success() {
            LogoutReq req = new LogoutReq();
            req.setRefreshToken("rt-xxx");
            when(request.getHeader("Authorization")).thenReturn("Bearer jwt-xxx");

            Result<Void> result = authController.logout(req);

            assertNotNull(result);
            verify(authService).logout(req, "jwt-xxx");
        }
    }

    @Nested
    class RefreshToken {

        @Test
        void refreshToken_success() {
            RefreshTokenReq req = new RefreshTokenReq();
            req.setRefreshToken("rt-xxx");
            RefreshTokenVO vo = RefreshTokenVO.builder()
                    .accessToken("new-jwt").refreshToken("new-rt")
                    .expiresInSeconds(1800L).build();
            when(authService.refreshToken(req)).thenReturn(vo);

            Result<RefreshTokenVO> result = authController.refreshToken(req);

            assertEquals("new-jwt", result.getData().accessToken());
            verify(authService).refreshToken(req);
        }
    }

    @Nested
    class GetUserTokens {

        @Test
        void getUserTokens_success() {
            TokenInfoVO vo = TokenInfoVO.builder().tokenId(1L).deviceInfo("Chrome").build();
            when(authService.getUserToken()).thenReturn(List.of(vo));

            Result<List<TokenInfoVO>> result = authController.getUserTokens();

            assertEquals(1, result.getData().size());
            assertEquals("Chrome", result.getData().get(0).deviceInfo());
            verify(authService).getUserToken();
        }
    }

    @Nested
    class Revoke {

        @Test
        void startRevokeChallenge_success() {
            SecureChallengeStartVO vo = new SecureChallengeStartVO(
                    "challenge-1", "138****8000", 300);
            when(authService.startRevokeChallenge(1L)).thenReturn(vo);

            Result<SecureChallengeStartVO> result =
                    authController.startRevokeChallenge(1L);

            assertEquals("challenge-1", result.getData().challengeId());
            verify(authService).startRevokeChallenge(1L);
        }

        @Test
        void revoke_success() {
            SecureActionContext context = SecureActionContext.builder().build();
            when(request.getAttribute(SecureActionContext.REQUEST_ATTRIBUTE)).thenReturn(context);

            Result<Void> result = authController.revoke(1L);

            assertNotNull(result);
            verify(authService).revoke(1L, context);
        }
    }

    @Nested
    class SwitchWorkspace {

        private final String authHeader = "Bearer old-jwt-token";

        @Test
        void switchWorkspace_success() {
            WorkspaceSwitchReq req = new WorkspaceSwitchReq();
            req.setWorkspaceType(WorkspaceType.ENTERPRISE);
            req.setEnterpriseId(200L);
            WorkspaceSwitchVO vo = new WorkspaceSwitchVO(
                    WorkspaceType.ENTERPRISE, 200L, "new-jwt", 1800L);
            when(request.getHeader("Authorization")).thenReturn(authHeader);
            when(authService.switchWorkspace(req, "old-jwt-token")).thenReturn(vo);

            Result<WorkspaceSwitchVO> result = authController.switchWorkspace(req);

            assertNotNull(result.getData());
            assertEquals("new-jwt", result.getData().accessToken());
            assertEquals(200L, result.getData().enterpriseId());
            assertEquals(1800L, result.getData().expiresInSeconds());
            verify(authService).switchWorkspace(req, "old-jwt-token");
        }
    }

    @Nested
    class GetUserInfo {

        @Test
        void getUserInfo_success() {
            UserEnterpriseVO enterprise = UserEnterpriseVO.builder()
                    .id(200L).name("测试企业").status(EnterpriseStatus.NORMAL).build();
            UserInfoBO bo = UserInfoBO.builder()
                    .id(1L).username("testuser").userType("CANDIDATE")
                    .enterpriseId(200L).enterpriseName("测试企业")
                    .enterprises(List.of(enterprise))
                    .status(UserStatus.NORMAL).roles(List.of("CANDIDATE"))
                    .permissions(List.of("candidate:resume:list")).build();
            UserInfoVO vo = UserInfoVO.builder()
                    .id(1L).username("testuser").userType("CANDIDATE")
                    .enterpriseId(200L).enterpriseName("测试企业")
                    .enterprises(List.of(enterprise))
                    .status(UserStatus.NORMAL).roles(List.of("CANDIDATE"))
                    .permissions(List.of("candidate:resume:list")).build();
            when(authService.getUserInfo()).thenReturn(bo);
            when(authConverter.toUserInfoVO(bo)).thenReturn(vo);

            Result<UserInfoVO> result = authController.getUserInfo();

            assertEquals(1L, result.getData().id());
            assertEquals("CANDIDATE", result.getData().userType());
            assertEquals(200L, result.getData().enterpriseId());
            assertEquals(List.of(enterprise), result.getData().enterprises());
            assertEquals(List.of("CANDIDATE"), result.getData().roles());
            assertEquals(List.of("candidate:resume:list"), result.getData().permissions());
            verify(authService).getUserInfo();
            verify(authConverter).toUserInfoVO(bo);
        }
    }
}
