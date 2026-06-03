package interview.system.auth;

import interview.common.constant.ApiVersion;
import interview.common.constant.Result;
import interview.system.auth.enums.SmsType;
import interview.system.auth.model.bo.RegisterBo;
import interview.system.auth.model.entity.UserToken;
import interview.system.auth.model.req.RegisterReq;
import interview.system.auth.model.req.SmsSendReq;
import interview.system.auth.model.vo.RegisterVO;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zhuxi
 * @apiNote 认证控制器
 * @since 2026/5/27 14:05
 */

@RestController
@RequestMapping(ApiVersion.V1 + "/auth")
@Tag(name = "认证")
@AllArgsConstructor
public class AuthController {

    private final SmsService smsService;
    private final AuthService authService;
    private final AuthConvertor authConvertor;

    @ApiResponse(description = "发送短信验证码")
    @PostMapping("/send-sms")
    public Result<?> sendSms(@RequestBody SmsSendReq smsReq) {
        smsService.sendSms(smsReq);
        return Result.success();
    }


    @ApiResponse(description = "用户注册")
    @PostMapping("/register")
    public Result<RegisterVO> register(@RequestBody RegisterReq register) {
        RegisterBo registerBo = authService.register(register);
        RegisterVO registerVO = authConvertor.toRegisterVO(registerBo);
        return Result.success(registerVO);
    }
}
