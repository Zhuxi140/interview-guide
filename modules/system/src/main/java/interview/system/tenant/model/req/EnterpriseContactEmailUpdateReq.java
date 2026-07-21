package interview.system.tenant.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 企业联系邮箱更新请求。
 */
@Data
@Schema(description = "企业联系邮箱更新请求")
public class EnterpriseContactEmailUpdateReq {

    @Schema(description = "企业联系邮箱", example = "hr@example.com")
    @NotBlank(message = "联系邮箱不能为空")
    @Pattern(
            regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
            message = "邮箱格式不正确")
    private String contactEmail;
}
