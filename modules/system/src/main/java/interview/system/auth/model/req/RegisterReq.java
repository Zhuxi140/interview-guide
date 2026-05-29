package interview.system.auth.model.req;

import interview.system.auth.enums.SmsType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 注册请求
 * @since 2026/5/27 14:05
 */

@Data
@Schema(description = "注册请求")
public class RegisterReq {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 64, message = "用户名长度 2~64 个字符")
    @Schema(description = "登录账号")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 128, message = "密码长度 6~128 个字符")
    @Schema(description = "密码")
    private String password;

    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱")
    private String email;

    @Pattern(regexp = "^\\d{11}$", message = "手机号格式不正确")
    @NotBlank(message = "手机号不能为空")
    @Schema(description = "手机号")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "验证码")
    private String code;

    @NotBlank(message = "用户类型不能为空")
    @Pattern(regexp = "^(HR|CANDIDATE)$", message = "用户类型仅支持 HR / CANDIDATE")
    @Schema(description = "用户类型：HR / CANDIDATE")
    private String userType;
}
