package interview.system.tenant.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 企业新联系电话验证码发送请求。
 * @author zhuxi
 */
@Data
@Schema(description = "企业新联系电话验证码发送请求")
public class EnterpriseContactNewPhoneReq {

    @Schema(description = "验证流程 ID", example = "f6a8d9c0873f4bbd93d271cf88f639b1")
    @NotBlank(message = "验证流程ID不能为空")
    private String flowId;

    @Schema(description = "待验证的企业新联系电话", example = "13800138000")
    @NotBlank(message = "新联系电话不能为空")
    @Pattern(regexp = "^\\d{11}$", message = "手机号格式不正确")
    private String newPhone;
}
