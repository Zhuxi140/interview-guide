package interview.system.api;

import interview.api.system.EnterpriseValidationApi;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.system.tenant.model.entity.Enterprise;
import interview.system.tenant.model.entity.EnterpriseTeamMember;
import interview.system.tenant.service.EnterprisesService;
import interview.system.tenant.service.EnterpriseTeamMembersService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class EnterpriseValidationApiImpl implements EnterpriseValidationApi {

    private final EnterprisesService enterprisesService;
    private final EnterpriseTeamMembersService enterpriseTeamMembersService;

    @Override
    public void validateEnterpriseBelong(Long enterpriseId,Long userId) {
        boolean exists = enterprisesService.lambdaQuery()
                .eq(Enterprise::getId, enterpriseId)
                .exists();
        if (!exists) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_FOUND);
        }

        Long userId = AuthContext.getRequiredUserId();
        boolean member = enterpriseTeamMembersService.lambdaQuery()
                .eq(EnterpriseTeamMember::getEnterpriseId, enterpriseId)
                .eq(EnterpriseTeamMember::getUserId, userId)
                .exists();
        if (!member) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_BELONG);
        }
    }
}
