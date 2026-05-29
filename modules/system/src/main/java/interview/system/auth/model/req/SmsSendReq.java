package interview.system.auth.model.req;

import interview.system.auth.enums.SmsType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 短信发送请求
 * @since 2026/5/27 14:05
 */

@Schema(description = "短信发送请求")
@Data
public class SmsSendReq {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^\\d{11}$", message = "手机号格式不正确")
    @Schema(description = "手机号")
    private String phone;

    @NotBlank(message = "短信类型不能为空")
    @Schema(description = "短信类型")
    private SmsType smsType;
}
