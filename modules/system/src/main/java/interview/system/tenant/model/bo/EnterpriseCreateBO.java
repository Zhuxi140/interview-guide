package interview.system.tenant.model.bo;

import interview.system.tenant.model.enums.EnterpriseStatus;
import lombok.Builder;

@Builder
public record EnterpriseCreateBO(
        Long id,
        String name,
        String shortName,
        String industry,
        String scale,
        EnterpriseStatus status,
        String logoUrl
){}

