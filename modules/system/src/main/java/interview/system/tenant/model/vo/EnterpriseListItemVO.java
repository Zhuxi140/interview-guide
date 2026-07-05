package interview.system.tenant.model.vo;

import interview.system.tenant.model.enums.EnterpriseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "企业列表项响应")
public record EnterpriseListItemVO(
    @Schema(description = "企业 ID")
    Long id,
    @Schema(description = "企业名称")
    String name,
    @Schema(description = "企业简称")
    String shortName,
    @Schema(description = "所属行业")
    String industry,
    @Schema(description = "企业状态 2:待认证 1:正常 0:暂停 -1:注销")
    EnterpriseStatus status,
    @Schema(description = "当前用户在企业的角色编码")
    String roleCode,
    @Schema(description = "企业成员数")
    Long memberCount
) {}
