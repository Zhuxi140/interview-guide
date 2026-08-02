package interview.system.tenant;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import interview.common.constant.AuthKeyConstant;
import interview.common.enums.ErrorCode;
import interview.common.enums.SmsType;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.system.auth.model.entity.User;
import interview.system.auth.service.SmsService;
import interview.system.auth.service.UsersService;
import interview.system.tenant.model.entity.Enterprise;
import interview.system.tenant.model.entity.EnterpriseTeamMember;
import interview.system.tenant.model.enums.EnterpriseContactVerifyStage;
import interview.system.tenant.model.enums.EnterpriseStatus;
import interview.system.tenant.model.req.EnterpriseContactNewPhoneReq;
import interview.system.tenant.model.vo.EnterpriseContactVerifyStartVO;
import interview.system.tenant.service.EnterpriseContactVerificationServiceImpl;
import interview.system.tenant.service.EnterprisesService;
import interview.system.tenant.service.EnterpriseTeamMembersService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.Map;

import static interview.system.TestMockUtils.mockQueryWrapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 企业联系电话验证流程单元测试。
 */
@ExtendWith(MockitoExtension.class)
class EnterpriseContactVerificationServiceImplTest {

    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private HashOperations<String, Object, Object> hashOperations;
    @Mock private SmsService smsService;
    @Mock private UsersService usersService;
    @Mock private EnterprisesService enterprisesService;
    @Mock private EnterpriseTeamMembersService enterpriseTeamMembersService;

    private EnterpriseContactVerificationServiceImpl service;

    private final Long userId = 1L;
    private final Long enterpriseId = 2L;

    @BeforeEach
    void setUp() {
        service = new EnterpriseContactVerificationServiceImpl(
                stringRedisTemplate,
                smsService,
                usersService,
                enterprisesService,
                enterpriseTeamMembersService);
        AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(userId)
                .build());
    }

    @AfterEach
    void tearDown() {
        AuthContext.remove();
    }

    @Test
    void start_shouldSendOldCode_whenEnterprisePhoneIsNotCurrentUserPhone() {
        mockStartDependencies("13800000001", "13900000002", 1L);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);

        EnterpriseContactVerifyStartVO result = service.start(enterpriseId);

        assertTrue(result.oldVerificationRequired());
        assertNotNull(result.flowId());
        verify(smsService).sendCode(
                eq("13800000001"),
                eq(SmsType.ENTERPRISE_VERIFY_OLD),
                contains(result.flowId()));
    }

    @Test
    void start_shouldSkipOldCode_whenEnterprisePhoneIsCurrentUserPhone() {
        mockStartDependencies("13800000001", "13800000001", 1L);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);

        EnterpriseContactVerifyStartVO result = service.start(enterpriseId);

        assertFalse(result.oldVerificationRequired());
        verifyNoInteractions(smsService);
    }

    @Test
    void start_shouldRejectUserOutsideEnterprise() {
        mockStartDependencies("13800000001", "13900000002", 0L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.start(enterpriseId));

        assertEquals(ErrorCode.ENTERPRISE_NOT_BELONG.getCode(), exception.getCode());
        verifyNoInteractions(smsService);
    }

    @Test
    void sendNewPhoneCode_shouldAllowPhoneWithoutPlatformAccount() {
        String flowId = "flow-1";
        mockEditableEnterprise(EnterpriseStatus.NORMAL, true, "13800000001");
        EnterpriseContactNewPhoneReq req = new EnterpriseContactNewPhoneReq();
        req.setFlowId(flowId);
        req.setNewPhone("13900000002");
        Map<Object, Object> flow = new HashMap<>();
        flow.put(AuthKeyConstant.ENTERPRISE_CONTACT_FLOW_FIELD_USER_ID, userId.toString());
        flow.put(AuthKeyConstant.ENTERPRISE_CONTACT_FLOW_FIELD_ENTERPRISE_ID, enterpriseId.toString());
        flow.put(AuthKeyConstant.ENTERPRISE_CONTACT_FLOW_FIELD_OLD_PHONE, "13800000001");
        flow.put(AuthKeyConstant.ENTERPRISE_CONTACT_FLOW_FIELD_NEW_PHONE, "");
        flow.put(
                AuthKeyConstant.ENTERPRISE_CONTACT_FLOW_FIELD_STAGE,
                EnterpriseContactVerifyStage.OLD_VERIFIED.name());
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(anyString())).thenReturn(flow);
        doReturn("5:").when(stringRedisTemplate).execute(
                any(), anyList(),
                anyString(), anyString(), anyString(), anyString(), anyString());

        service.sendNewPhoneCode(enterpriseId, req);

        verify(smsService).sendCode(
                eq("13900000002"),
                eq(SmsType.ENTERPRISE_VERIFY_NEW),
                contains(flowId));
        verifyNoInteractions(usersService);
    }

    @Test
    void sendNewPhoneCode_shouldRejectPausedEnterprise() {
        mockEditableEnterprise(EnterpriseStatus.PAUSED, true, "13800000001");
        EnterpriseContactNewPhoneReq req = new EnterpriseContactNewPhoneReq();
        req.setFlowId("flow-1");
        req.setNewPhone("13900000002");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.sendNewPhoneCode(enterpriseId, req));

        assertEquals(ErrorCode.ENTERPRISE_FROZEN.getCode(), exception.getCode());
        verifyNoInteractions(smsService);
    }

    private void mockStartDependencies(
            String enterprisePhone, String userPhone, Long memberCount) {
        mockEditableEnterprise(
                EnterpriseStatus.NORMAL, memberCount > 0, enterprisePhone);
        if (memberCount > 0) {
            LambdaQueryChainWrapper<User> query = mockQueryWrapper();
            when(usersService.lambdaQuery()).thenReturn(query);
            when(query.one()).thenReturn(User.builder()
                    .id(userId)
                    .phone(userPhone)
                    .build());
        }
    }

    private void mockEditableEnterprise(
            EnterpriseStatus status, boolean member, String enterprisePhone) {
        LambdaQueryChainWrapper<Enterprise> enterpriseQuery = mockQueryWrapper();
        when(enterprisesService.lambdaQuery()).thenReturn(enterpriseQuery);
        when(enterpriseQuery.one()).thenReturn(
                Enterprise.builder()
                        .id(enterpriseId)
                        .contactPhone(enterprisePhone)
                        .status(status)
                        .build());
        if (status == EnterpriseStatus.PENDING || status == EnterpriseStatus.NORMAL) {
            LambdaQueryChainWrapper<EnterpriseTeamMember> memberQuery = mockQueryWrapper();
            when(enterpriseTeamMembersService.lambdaQuery()).thenReturn(memberQuery);
            when(memberQuery.exists()).thenReturn(member);
        }
    }
}
