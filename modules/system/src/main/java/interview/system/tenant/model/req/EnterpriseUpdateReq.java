package interview.system.tenant.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 更新企业信息请求
 * @since 2026/5/27 14:05
 */

@Data
@Schema(description = "更新企业信息请求")
public class EnterpriseUpdateReq {

    @Size(min = 2, max = 128, message = "企业名称长度 2~128 个字符")
    @Schema(description = "企业名称")
    private String name;

    @Size(max = 64, message = "企业简称长度不超过 64 个字符")
    @Schema(description = "企业简称")
    private String shortName;

    @Schema(description = "所属行业")
    private String industry;

    @Schema(description = "企业规模")
    private String scale;

    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "邮箱格式不正确")
    @Schema(description = "联系邮箱")
    private String contactEmail;

    @Pattern(regexp = "^\\d{11}$", message = "手机号格式不正确")
    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "企业 Logo URL")
    private String logoUrl;
}
