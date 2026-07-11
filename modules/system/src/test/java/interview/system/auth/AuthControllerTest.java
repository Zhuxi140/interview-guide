package interview.system.auth;

import interview.common.constant.Result;
import interview.common.enums.SmsType;
import interview.system.auth.model.bo.LoginBO;
import interview.system.auth.model.bo.RegisterBo;
import interview.system.auth.model.bo.UserInfoBO;
import interview.system.auth.model.enums.UserStatus;
import interview.system.auth.model.req.*;
import interview.system.auth.model.vo.*;
import interview.system.auth.service.AuthService;
import interview.system.auth.service.SmsService;
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
    private AuthService authService;
    @Mock
    private AuthConverter authConverter;
    @Mock
    private HttpServletRequest request;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(smsService, authService, authConverter, request);
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
    class VerifyCode {

        @Test
        void verifyCode_success() {
            VerifyReq req = new VerifyReq();
            req.setPhone("13800138000");
            req.setCode("123456");
            req.setSmsType(SmsType.LOGIN);
            when(smsService.verifyForSensitiveAction(req)).thenReturn("token-xxx");

            Result<String> result = authController.verifyCode(req);

            assertEquals("token-xxx", result.getData());
            verify(smsService).verifyForSensitiveAction(req);
        }
    }

    @Nested
    class Register {

        private final RegisterReq req = new RegisterReq();
        private final RegisterBo bo = RegisterBo.builder()
                .userId(1L).username("testuser").accessToken("jwt-xxx")
                .refreshToken("rt-xxx").expiresIn(1800L).build();
        private final RegisterVO vo = RegisterVO.builder()
                .userId(1L).username("testuser").accessToken("jwt-xxx")
                .refreshToken("rt-xxx").expiresIn(1800L).build();

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
                .refreshToken("rt-xxx").expiresIn(1800L).build();
        private final LoginVO vo = LoginVO.builder()
                .userId(1L).username("testuser").accessToken("jwt-xxx")
                .refreshToken("rt-xxx").expiresIn(1800L).build();

        @BeforeEach
        void setUp() {
            req.setUsername("testuser");
            req.setPassword("pass123");
        }

        @Test
        void login_success() {
            when(authService.login(req)).thenReturn(bo);
            when(authConverter.toLoginVO(bo)).thenReturn(vo);

            Result<LoginVO> result = authController.login(req);

            assertNotNull(result.getData());
            assertEquals("jwt-xxx", result.getData().accessToken());
            verify(authService).login(req);
            verify(authConverter).toLoginVO(bo);
        }
    }

    @Nested
    class Logout {

        @Test
        void logout_success() {
            LogoutReq req = new LogoutReq();
            req.setRefreshToken("rt-xxx");
            req.setAccessToken("jwt-xxx");

            Result<Void> result = authController.logout(req);

            assertNotNull(result);
            verify(authService).logout(req);
        }
    }

    @Nested
    class RefreshToken {

        @Test
        void refreshToken_success() {
            RefreshTokenReq req = new RefreshTokenReq();
            req.setRefreshToken("rt-xxx");
            req.setAccessToken("jwt-xxx");
            RefreshTokenVO vo = RefreshTokenVO.builder()
                    .accessToken("new-jwt").refreshToken("new-rt").expiresIn(1800L).build();
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
        void revoke_success() {
            RevokeDeviceReq req = new RevokeDeviceReq("123456");

            Result<Void> result = authController.revoke(1L, req);

            assertNotNull(result);
            verify(authService).revoke(1L, req);
        }
    }

    @Nested
    class SwitchEnterprise {

        private final String authHeader = "Bearer old-jwt-token";

        @Test
        void switchEnterprise_success() {
            SwitchEnterpriseVO vo = SwitchEnterpriseVO.builder()
                    .accessToken("new-jwt").expiresIn(1800L).build();
            when(request.getHeader("Authorization")).thenReturn(authHeader);
            when(authService.switchEnterprise(200L, "old-jwt-token")).thenReturn(vo);

            Result<SwitchEnterpriseVO> result = authController.switchEnterprise(200L);

            assertNotNull(result.getData());
            assertEquals("new-jwt", result.getData().accessToken());
            assertEquals(1800L, result.getData().expiresIn());
            verify(authService).switchEnterprise(200L, "old-jwt-token");
        }
    }

    @Nested
    class GetUserInfo {

        @Test
        void getUserInfo_success() {
            UserInfoBO bo = UserInfoBO.builder()
                    .id(1L).username("testuser").userType("CANDIDATE")
                    .status(UserStatus.NORMAL).build();
            UserInfoVO vo = UserInfoVO.builder()
                    .id(1L).username("testuser").userType("CANDIDATE")
                    .status(UserStatus.NORMAL).build();
            when(authService.getUserInfo()).thenReturn(bo);
            when(authConverter.toUserInfoVO(bo)).thenReturn(vo);

            Result<UserInfoVO> result = authController.getUserInfo();

            assertEquals(1L, result.getData().id());
            assertEquals("CANDIDATE", result.getData().userType());
            verify(authService).getUserInfo();
            verify(authConverter).toUserInfoVO(bo);
        }
    }
}
