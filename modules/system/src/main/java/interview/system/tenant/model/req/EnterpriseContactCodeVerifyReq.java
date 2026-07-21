package interview.system.tenant.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 企业联系电话验证码校验请求。
 */
@Data
@Schema(description = "企业联系电话验证码校验请求")
public class EnterpriseContactCodeVerifyReq {

    @Schema(description = "验证流程 ID", example = "f6a8d9c0873f4bbd93d271cf88f639b1")
    @NotBlank(message = "验证流程ID不能为空")
    private String flowId;

    @Schema(description = "六位短信验证码", example = "123456")
    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码格式不正确")
    private String code;
}
