package interview.system.tenant.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "更新企业联系方式响应")
public record EnterpriseContactUpdateVO(
    @Schema(description = "企业ID")
    Long id,
    @Schema(description = "联系邮箱")
    String contactEmail,
    @Schema(description = "联系电话")
    String contactPhone
) {}
