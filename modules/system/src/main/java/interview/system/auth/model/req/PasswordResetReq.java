package interview.system.auth.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 短信验证码重置密码请求。
 */
@Data
@Schema(description = "短信验证码重置密码请求")
public class PasswordResetReq {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^\\d{11}$", message = "手机号格式不正确")
    @Schema(description = "已注册手机号", example = "13800138000")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "重置密码短信验证码", example = "123456")
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 128, message = "密码长度 6~128 个字符")
    @Schema(description = "新密码")
    private String newPassword;
}
