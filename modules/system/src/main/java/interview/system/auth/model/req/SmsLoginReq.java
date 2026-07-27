package interview.system.auth.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 手机验证码登录请求。
 */
@Data
@Schema(description = "手机验证码登录请求")
public class SmsLoginReq {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^\\d{11}$", message = "手机号格式不正确")
    @Schema(description = "已注册手机号", example = "13800138000")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "登录短信验证码", example = "123456")
    private String code;

    @Schema(description = "设备 UA")
    private String deviceInfo;
}
