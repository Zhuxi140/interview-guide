package interview.system.tenant.model.bo;

import interview.system.tenant.model.enums.EnterpriseStatus;
import lombok.Builder;

@Builder
public record ListUserEnterprisesBO(
    Long enterpriseId,
    String name,
    String shortName,
    String industry,
    EnterpriseStatus status,
    Integer roleId,
    String roleCode,
    Long memberCount
) {}
