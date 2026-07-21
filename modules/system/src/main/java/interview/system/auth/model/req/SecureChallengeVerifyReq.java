package interview.system.auth.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 安全验证 Challenge 核销请求。
 */
@Data
@Schema(description = "安全验证 Challenge 核销请求")
public class SecureChallengeVerifyReq {

    @Schema(description = "六位短信验证码", example = "123456")
    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码格式不正确")
    private String code;
}
