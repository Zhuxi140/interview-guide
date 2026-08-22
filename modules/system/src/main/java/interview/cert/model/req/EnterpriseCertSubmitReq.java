package interview.cert.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交企业资质认证请求。
 */
@Data
@Schema(description = "提交企业资质认证请求")
public class EnterpriseCertSubmitReq {

    @NotBlank(message = "统一社会信用代码不能为空")
    @Size(max = 64, message = "统一社会信用代码不能超过 64 个字符")
    @Schema(description = "统一社会信用代码（18 位）",
            example = "91330100MA27X8901X")
    private String creditCode;

    @NotBlank(message = "法定代表人姓名不能为空")
    @Size(max = 64, message = "法定代表人姓名不能超过 64 个字符")
    @Schema(description = "法定代表人姓名", example = "张三")
    private String legalPerson;

    @NotBlank(message = "营业执照材料令牌不能为空")
    @Size(max = 512, message = "营业执照材料令牌不能超过 512 个字符")
    @Schema(description = "经上传凭证签发、归属当前企业的营业执照材料令牌",
            example = "ENTERPRISE_CERT/2026/08/22/3f2a9d8c_business-license.jpg")
    private String licenseMaterialToken;
}
