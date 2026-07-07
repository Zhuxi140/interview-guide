package interview.system.tenant.model.bo;

import interview.system.tenant.model.enums.EnterpriseStatus;

public record UserEnterprisesBO(
        Long enterpriseId,
        String name,
        String shortName,
        String industry,
        EnterpriseStatus status,
        Integer roleId
) {}
