package interview.system.auth;

import interview.common.constant.ApiVersion;
import interview.common.constant.Result;
import interview.common.enums.RiskLevel;
import interview.framework.annonate.MaxRiskLevel;
import interview.system.auth.model.bo.LoginBO;
import interview.system.auth.model.bo.RegisterBo;
import interview.system.auth.model.bo.UserInfoBO;
import interview.system.auth.model.req.*;
import interview.system.auth.model.vo.*;
import interview.system.auth.service.AuthService;
import interview.system.auth.service.SmsService;
import cn.hutool.core.util.StrUtil;
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
    @Operation(summary = "敏感操作授权令牌")
    @PostMapping("/verify-sms")
    public Result<String> verifyCode(@RequestBody @Valid VerifyReq verifyReq) {
        String token = smsService.verifyForSensitiveAction(verifyReq);
        return Result.success(token);
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
        LoginBO login = authService.login(loginReq);
        LoginVO loginVO = authConverter.toLoginVO(login);
        return Result.success(loginVO);
    }

    @MaxRiskLevel(RiskLevel.HIGH_RISK)
    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestBody @Valid LogoutReq logout) {
        authService.logout(logout);
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
    @Operation(summary = "注销当前用户的某个设备")
    @PostMapping("/revoke/{tokenId}")
    public Result<Void> revoke(@PathVariable Long tokenId, @RequestBody @Valid RevokeDeviceReq code ) {
        authService.revoke(tokenId, code);
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
