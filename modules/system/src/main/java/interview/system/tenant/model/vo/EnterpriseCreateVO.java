package interview.system.tenant.model.vo;

import interview.system.tenant.model.enums.EnterpriseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "创建企业响应")
public record EnterpriseCreateVO(
    @Schema(description = "企业 ID")
    Long id,
    @Schema(description = "企业名称")
    String name,
    @Schema(description = "企业简称")
    String shortName,
    @Schema(description = "企业状态 2:待认证 1:正常 0:暂停 -1:注销")
    EnterpriseStatus status
) {}
