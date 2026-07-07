package interview.system.tenant.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * @author zhuxi
 */
@Data
@Schema(description = "更新企业联系方式请求")
public class EnterpriseContactUpdateReq {

    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "邮箱格式不正确")
    @Schema(description = "联系邮箱")
    private String contactEmail;

    @Pattern(regexp = "^\\d{11}$", message = "手机号格式不正确")
    @Schema(description = "联系电话")
    private String contactPhone;
}
