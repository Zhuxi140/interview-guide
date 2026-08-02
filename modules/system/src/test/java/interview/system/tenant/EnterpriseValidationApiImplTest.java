package interview.system.tenant;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.system.tenant.api.EnterpriseValidationApiImpl;
import interview.system.tenant.model.entity.Enterprise;
import interview.system.tenant.model.entity.EnterpriseTeamMember;
import interview.system.tenant.model.enums.EnterpriseStatus;
import interview.system.tenant.service.EnterpriseTeamMembersService;
import interview.system.tenant.service.EnterprisesService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static interview.system.TestMockUtils.mockQueryWrapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnterpriseValidationApiImplTest {

    @Mock private EnterprisesService enterprisesService;
    @Mock private EnterpriseTeamMembersService enterpriseTeamMembersService;

    @Test
    void validateActiveEnterpriseBelong_rejectsPendingEnterprise() {
        LambdaQueryChainWrapper<Enterprise> enterpriseQuery = mockQueryWrapper();
        when(enterpriseQuery.one()).thenReturn(Enterprise.builder()
                .id(1L)
                .status(EnterpriseStatus.PENDING)
                .build());
        when(enterprisesService.lambdaQuery()).thenReturn(enterpriseQuery);

        LambdaQueryChainWrapper<EnterpriseTeamMember> memberQuery = mockQueryWrapper();
        when(memberQuery.exists()).thenReturn(true);
        when(enterpriseTeamMembersService.lambdaQuery()).thenReturn(memberQuery);

        EnterpriseValidationApiImpl api = new EnterpriseValidationApiImpl(
                enterprisesService, enterpriseTeamMembersService);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> api.validateActiveEnterpriseBelong(1L, 2L));

        assertEquals(ErrorCode.ENTERPRISE_NOT_CERTIFIED.getCode(), exception.getCode());
    }
}
