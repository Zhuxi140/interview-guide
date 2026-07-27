package interview.system.auth;

import cn.hutool.core.util.StrUtil;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequireSecure;
import interview.common.constant.ApiVersion;
import interview.common.constant.Result;
import interview.common.constant.SecureActionContext;
import interview.common.enums.RiskLevel;
import interview.common.enums.SecureActionType;
import interview.system.auth.model.bo.LoginBO;
import interview.system.auth.model.bo.RegisterBo;
import interview.system.auth.model.bo.UserInfoBO;
import interview.system.auth.model.req.*;
import interview.system.auth.model.vo.*;
import interview.system.auth.service.AuthService;
import interview.system.auth.service.SecureChallengeService;
import interview.system.auth.service.SmsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author zhuxi
 * @apiNote 认证控制器
 */

@RestController
@RequestMapping(ApiVersion.V1 + "/auth")
@Tag(name = "认证")
@AllArgsConstructor
public class AuthController {

    private final SmsService smsService;
    private final SecureChallengeService secureChallengeService;
    private final AuthService authService;
    private final AuthConverter authConverter;
    private final HttpServletRequest request;

    @Operation(summary = "发送短信验证码")
    @PostMapping("/send-sms")
    public Result<?> sendSms(@RequestBody @Valid SmsSendReq smsReq) {
        smsService.sendSms(smsReq);
        return Result.success();
    }

    @MaxRiskLevel(RiskLevel.HIGH_RISK)
    @Operation(summary = "核销安全验证 Challenge 并签发一次性令牌")
    @PostMapping("/secure-challenges/{challengeId}/verify")
    public Result<SecureActionTokenVO> verifySecureChallenge(
            @PathVariable("challengeId") String challengeId,
            @RequestBody @Valid SecureChallengeVerifyReq req) {
        // Challenge 决定验证手机号、业务动作和资源，客户端只提交验证码
        return Result.success(secureChallengeService.verify(challengeId, req));
    }


    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<RegisterVO> register(@RequestBody @Valid RegisterReq register) {
        RegisterBo registerBo = authService.register(register);
        RegisterVO registerVO = authConverter.toRegisterVO(registerBo);
        return Result.success(registerVO);
    }


    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginReq loginReq) {
        LoginBO login = authService.login(loginReq, request.getRemoteAddr());
        LoginVO loginVO = authConverter.toLoginVO(login);
        return Result.success(loginVO);
    }

    @Operation(summary = "手机号验证码登录")
    @PostMapping("/login/sms")
    public Result<LoginVO> loginBySms(@RequestBody @Valid SmsLoginReq loginReq) {
        // 客户端 IP 只从服务端请求上下文取得，禁止客户端请求体覆盖。
        LoginBO login = authService.loginBySms(loginReq, request.getRemoteAddr());
        return Result.success(authConverter.toLoginVO(login));
    }

    @Operation(summary = "使用短信验证码重置密码")
    @PostMapping("/password/reset")
    public Result<Void> resetPassword(@RequestBody @Valid PasswordResetReq resetReq) {
        authService.resetPassword(resetReq);
        return Result.success();
    }

    @MaxRiskLevel(RiskLevel.HIGH_RISK)
    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestBody @Valid LogoutReq logout) {
        String accessToken = StrUtil.removePrefix(request.getHeader("Authorization"), "Bearer ");
        authService.logout(logout, accessToken);
        return Result.success();
    }

    @MaxRiskLevel(RiskLevel.HIGH_RISK)
    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public Result<RefreshTokenVO> refreshToken(@RequestBody @Valid RefreshTokenReq refreshToken) {
        return Result.success(authService.refreshToken(refreshToken));
    }

    @MaxRiskLevel(RiskLevel.HIGH_RISK)
    @Operation(summary = "查询当前用户的活跃设备列表")
    @GetMapping("/tokens")
    public Result<List<TokenInfoVO>> getUserTokens() {
        return Result.success(authService.getUserToken());
    }

    @MaxRiskLevel(RiskLevel.MID_RISK)
    @Operation(summary = "创建下线设备 Challenge")
    @PostMapping("/tokens/{tokenId}/revoke/challenge")
    public Result<SecureChallengeStartVO> startRevokeChallenge(
            @PathVariable("tokenId") Long tokenId) {
        // 绑定当前用户和目标设备 Token，向用户绑定手机号发送验证码
        return Result.success(authService.startRevokeChallenge(tokenId));
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequireSecure(SecureActionType.REVOKE_DEVICE)
    @Operation(summary = "注销当前用户的某个设备，需携带设备下线令牌")
    @PostMapping("/revoke/{tokenId}")
    public Result<Void> revoke(@PathVariable Long tokenId) {
        // 读取拦截器已校验并消费的设备下线安全上下文
        SecureActionContext context = (SecureActionContext) request.getAttribute(
                SecureActionContext.REQUEST_ATTRIBUTE);
        authService.revoke(tokenId, context);
        return Result.success();
    }

    @MaxRiskLevel(RiskLevel.MID_RISK)
    @Operation(summary = "切换当前活跃企业（旧 JWT 将被加入黑名单）")
    @PutMapping("/enterprise/{enterpriseId}")
    public Result<SwitchEnterpriseVO> switchEnterprise(@PathVariable Long enterpriseId) {
        String header = request.getHeader("Authorization");
        String accessToken = StrUtil.removePrefix(header, "Bearer ");
        return Result.success(authService.switchEnterprise(enterpriseId, accessToken));
    }

    @MaxRiskLevel(RiskLevel.HIGH_RISK)
    @Operation(summary = "查询当前用户信息")
    @GetMapping("/me")
    public Result<UserInfoVO> getUserInfo() {
        UserInfoBO userInfo = authService.getUserInfo();
        UserInfoVO userInfoVO = authConverter.toUserInfoVO(userInfo);

        return Result.success(userInfoVO);
    }
}
