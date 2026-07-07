package interview.system.auth.model.req;

import interview.common.enums.SmsType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * @author zhuxi
 */

@Data
public class VerifyReq {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^\\d{11}$", message = "手机号格式不正确")
    @Schema(description = "手机号")
    String phone;
    @NotBlank(message = "手机号验证码不能为空")
    String code;
    @NotNull(message = "短信类型不能为空")
    @Schema(description = "短信类型")
    SmsType smsType;

    @Schema(description = "二次手机验证中，第一次手机验证获取的令牌")
    String previousToken;
}
