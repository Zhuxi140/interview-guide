package interview.system.tenant.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author zhuxi
 */
@Data
@Schema(description = "更新企业基本信息请求")
public class EnterpriseBasicUpdateReq {

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

    @Schema(description = "企业 Logo URL")
    private String logoUrl;
}
