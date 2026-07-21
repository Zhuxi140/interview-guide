package interview.system.tenant;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import interview.api.system.SecureChallengeApi;
import interview.common.constant.SecureActionContext;
import interview.common.enums.ErrorCode;
import interview.common.enums.SecureActionType;
import interview.common.exception.BusinessException;
import interview.framework.config.CustomIdGenerator;
import interview.framework.context.AuthContext;
import interview.system.rbac.model.entity.Role;
import interview.system.rbac.service.RolesService;
import interview.system.rbac.service.UserRolesService;
import interview.system.tenant.mapper.EnterprisesMapper;
import interview.system.tenant.model.bo.EnterpriseCreateBO;
import interview.system.tenant.model.bo.ListUserEnterprisesBO;
import interview.system.tenant.model.bo.UserEnterprisesBO;
import interview.system.tenant.model.entity.Enterprise;
import interview.system.tenant.model.entity.EnterpriseTeamMember;
import interview.system.tenant.model.enums.EnterpriseStatus;
import interview.system.tenant.model.req.EnterpriseBasicUpdateReq;
import interview.system.tenant.model.req.EnterpriseContactEmailUpdateReq;
import interview.system.tenant.model.req.EnterpriseCreateReq;
import interview.system.tenant.model.vo.EnterpriseContactEmailUpdateVO;
import interview.system.tenant.model.vo.EnterpriseContactPhoneUpdateVO;
import interview.system.tenant.model.vo.EnterpriseDetailVO;
import interview.system.tenant.model.vo.EnterpriseUpdateVO;
import interview.system.tenant.service.EnterpriseTeamMembersService;
import interview.system.tenant.service.EnterprisesServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static interview.system.TestMockUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnterprisesServiceImplTest {

    @Mock private CustomIdGenerator customIdGenerator;
    @Mock private UserRolesService userRolesService;
    @Mock private EnterpriseTeamMembersService enterpriseTeamMembersService;
    @Mock private EnterprisesMapper enterprisesMapper;
    @Mock private RolesService rolesService;
    @Mock private SecureChallengeApi secureChallengeApi;

    private EnterprisesServiceImpl enterprisesService;
    private LambdaQueryChainWrapper<Enterprise> enterpriseQueryWrapper;
    private LambdaUpdateChainWrapper<Enterprise> enterpriseUpdateWrapper;

    private final Long userId = 1L;
    private final Long enterpriseId = 10001L;
    private final String enterpriseName = "测试企业";
    private final String shortName = "测试";
    private final String industry = "互联网";
    private final String contactEmail = "contact@test.com";
    private final String contactPhone = "13812345678";

    @BeforeEach
    void setUp() {
        enterprisesService = spy(new EnterprisesServiceImpl(
            customIdGenerator, userRolesService, enterpriseTeamMembersService,
            enterprisesMapper, rolesService, secureChallengeApi
        ));
        ReflectionTestUtils.setField(enterprisesService, "baseMapper", enterprisesMapper);
        enterpriseQueryWrapper = mockQueryWrapper();
        enterpriseUpdateWrapper = mockUpdateWrapper();
    }

    @AfterEach
    void tearDown() {
        AuthContext.remove();
    }

    // ============================== createEnterprise ==============================

    @Nested
    class CreateEnterprise {

        private EnterpriseCreateReq req;

        @BeforeEach
        void setUp() {
            AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(userId).build());
            req = new EnterpriseCreateReq();
            req.setName(enterpriseName);
            req.setShortName(shortName);
            req.setIndustry(industry);
            req.setContactEmail(contactEmail);
            req.setContactPhone(contactPhone);
        }

        @Test
        void createEnterprise_success() {
            doReturn(enterpriseQueryWrapper).when(enterprisesService).lambdaQuery();
            when(enterpriseQueryWrapper.exists()).thenReturn(false);
            when(customIdGenerator.nextId(any())).thenReturn(enterpriseId);
            doReturn(true).when(enterprisesService).save(any(Enterprise.class));
            when(enterpriseTeamMembersService.save(any(EnterpriseTeamMember.class))).thenReturn(true);

            EnterpriseCreateBO result = enterprisesService.createEnterprise(req);

            assertNotNull(result);
            assertEquals(enterpriseId, result.id());
            assertEquals(enterpriseName, result.name());
            assertEquals(EnterpriseStatus.PENDING, result.status());
            verify(enterprisesService).save(any(Enterprise.class));
            verify(enterpriseTeamMembersService).save(any());
        }

        @Test
        void createEnterprise_fail_nameAlreadyExists() {
            doReturn(enterpriseQueryWrapper).when(enterprisesService).lambdaQuery();
            when(enterpriseQueryWrapper.exists()).thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class, () -> enterprisesService.createEnterprise(req));
            assertEquals(ErrorCode.ENTERPRISE_NAME_ALREADY_EXISTS.getCode(), ex.getCode());
        }

        @Test
        void createEnterprise_fail_nameConflictDataIntegrity() {
            doReturn(enterpriseQueryWrapper).when(enterprisesService).lambdaQuery();
            when(enterpriseQueryWrapper.exists()).thenReturn(false);
            when(customIdGenerator.nextId(any())).thenReturn(enterpriseId);
            doThrow(DataIntegrityViolationException.class).when(enterprisesService).save(any(Enterprise.class));

            BusinessException ex = assertThrows(BusinessException.class, () -> enterprisesService.createEnterprise(req));
            assertEquals(ErrorCode.ENTERPRISE_NAME_ALREADY_EXISTS.getCode(), ex.getCode());
        }
    }

    // ============================== listUserEnterprises ==============================

    @Nested
    class ListUserEnterprises {

        @BeforeEach
        void setUp() {
            AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(userId).build());
        }

        @Test
        void listUserEnterprises_success_withRoleCodeAndMemberCount() {
            UserEnterprisesBO bo1 = new UserEnterprisesBO(enterpriseId, enterpriseName, shortName, industry, EnterpriseStatus.NORMAL, interview.common.enums.Role.ENTERPRISE_OWNER.getCode());
            UserEnterprisesBO bo2 = new UserEnterprisesBO(20002L, "另一企业", "另一", "教育", EnterpriseStatus.PENDING, interview.common.enums.Role.ENTERPRISE_ADMIN.getCode());
            when(enterprisesMapper.getListUserEnterprises(userId)).thenReturn(List.of(bo1, bo2));

            Role role1 = new Role();
            role1.setId(interview.common.enums.Role.ENTERPRISE_OWNER.getCode());
            role1.setRoleCode("ENTERPRISE_OWNER");
            Role role2 = new Role();
            role2.setId(interview.common.enums.Role.ENTERPRISE_ADMIN.getCode());
            role2.setRoleCode("ENTERPRISE_ADMIN");
            LambdaQueryChainWrapper<Role> roleQueryWrapper = mockQueryWrapper();
            when(roleQueryWrapper.list()).thenReturn(List.of(role1, role2));
            when(rolesService.lambdaQuery()).thenReturn(roleQueryWrapper);

            when(enterpriseTeamMembersService.listMaps(any(QueryWrapper.class))).thenReturn(
                List.of(Map.of("enterprise_id", enterpriseId, "count", 5L),
                        Map.of("enterprise_id", 20002L, "count", 3L))
            );

            List<ListUserEnterprisesBO> result = enterprisesService.listUserEnterprises();

            assertEquals(2, result.size());
            assertEquals(enterpriseId, result.get(0).enterpriseId());
            assertEquals("ENTERPRISE_OWNER", result.get(0).roleCode());
            assertEquals(5L, result.get(0).memberCount());
            assertEquals("ENTERPRISE_ADMIN", result.get(1).roleCode());
            assertEquals(3L, result.get(1).memberCount());
        }

        @Test
        void listUserEnterprises_empty() {
            when(enterprisesMapper.getListUserEnterprises(userId)).thenReturn(List.of());

            List<ListUserEnterprisesBO> result = enterprisesService.listUserEnterprises();

            assertTrue(result.isEmpty());
        }
    }

    // ============================== getEnterpriseDetail ==============================

    @Nested
    class GetEnterpriseDetail {

        @BeforeEach
        void setUp() {
            AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(userId).build());
        }

        @Test
        void getEnterpriseDetail_success() {
            doReturn(enterpriseQueryWrapper).when(enterprisesService).lambdaQuery();
            when(enterpriseQueryWrapper.exists()).thenReturn(true);

            LambdaQueryChainWrapper<EnterpriseTeamMember> memberQueryWrapper = mockQueryWrapper();
            when(memberQueryWrapper.exists()).thenReturn(true);
            when(enterpriseTeamMembersService.lambdaQuery()).thenReturn(memberQueryWrapper);

            Enterprise enterprise = Enterprise.builder()
                .id(enterpriseId).name(enterpriseName).shortName(shortName)
                .industry(industry).scale("100-500")
                .contactEmail(contactEmail).contactPhone(contactPhone)
                .status(EnterpriseStatus.NORMAL).logoUrl("http://logo.url")
                .createdAt(OffsetDateTime.now())
                .build();
            when(enterprisesMapper.getEnterpriseDetail(enterpriseId)).thenReturn(enterprise);

            EnterpriseDetailVO result = enterprisesService.getEnterpriseDetail(enterpriseId);

            assertNotNull(result);
            assertEquals(enterpriseId, result.id());
            assertEquals(enterpriseName, result.name());
            assertEquals(EnterpriseStatus.NORMAL, result.status());
        }

        @Test
        void getEnterpriseDetail_fail_enterpriseNotFound() {
            doReturn(enterpriseQueryWrapper).when(enterprisesService).lambdaQuery();
            when(enterpriseQueryWrapper.exists()).thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class, () -> enterprisesService.getEnterpriseDetail(enterpriseId));
            assertEquals(ErrorCode.ENTERPRISE_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        void getEnterpriseDetail_fail_userNotBelong() {
            doReturn(enterpriseQueryWrapper).when(enterprisesService).lambdaQuery();
            when(enterpriseQueryWrapper.exists()).thenReturn(true);

            LambdaQueryChainWrapper<EnterpriseTeamMember> memberQueryWrapper = mockQueryWrapper();
            when(memberQueryWrapper.exists()).thenReturn(false);
            when(enterpriseTeamMembersService.lambdaQuery()).thenReturn(memberQueryWrapper);

            BusinessException ex = assertThrows(BusinessException.class, () -> enterprisesService.getEnterpriseDetail(enterpriseId));
            assertEquals(ErrorCode.ENTERPRISE_NOT_BELONG.getCode(), ex.getCode());
        }
    }

    // ============================== updateEnterpriseBasic ==============================

    @Nested
    class UpdateEnterpriseBasic {

        private EnterpriseBasicUpdateReq req;

        @BeforeEach
        void setUp() {
            AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(userId).build());
            req = new EnterpriseBasicUpdateReq();
        }

        @Test
        void updateEnterpriseBasic_success_partialFields() {
            doReturn(enterpriseQueryWrapper).when(enterprisesService).lambdaQuery();
            when(enterpriseQueryWrapper.exists()).thenReturn(true);
            LambdaQueryChainWrapper<EnterpriseTeamMember> memberQueryWrapper = mockQueryWrapper();
            when(memberQueryWrapper.exists()).thenReturn(true);
            when(enterpriseTeamMembersService.lambdaQuery()).thenReturn(memberQueryWrapper);

            req.setName("新企业名");
            req.setShortName("新简称");

            Enterprise updated = Enterprise.builder()
                .id(enterpriseId).name("新企业名").shortName("新简称")
                .industry(industry).build();
            when(enterprisesMapper.updateById(any(Enterprise.class))).thenReturn(1);
            when(enterprisesMapper.selectById(enterpriseId)).thenReturn(updated);

            EnterpriseUpdateVO result = enterprisesService.updateEnterpriseBasic(enterpriseId, req);

            assertNotNull(result);
            assertEquals(enterpriseId, result.id());
            assertEquals("新企业名", result.name());
            assertEquals("新简称", result.shortName());
            verify(enterprisesMapper).updateById(any(Enterprise.class));
        }

        @Test
        void updateEnterpriseBasic_success_allFields() {
            doReturn(enterpriseQueryWrapper).when(enterprisesService).lambdaQuery();
            when(enterpriseQueryWrapper.exists()).thenReturn(true);
            LambdaQueryChainWrapper<EnterpriseTeamMember> memberQueryWrapper = mockQueryWrapper();
            when(memberQueryWrapper.exists()).thenReturn(true);
            when(enterpriseTeamMembersService.lambdaQuery()).thenReturn(memberQueryWrapper);

            req.setName("新企业名");
            req.setShortName("新简称");
            req.setIndustry("金融");
            req.setScale("1000-5000");
            req.setLogoUrl("http://new.logo");

            Enterprise updated = Enterprise.builder()
                .id(enterpriseId).name("新企业名").shortName("新简称")
                .industry("金融").build();
            when(enterprisesMapper.updateById(any(Enterprise.class))).thenReturn(1);
            when(enterprisesMapper.selectById(enterpriseId)).thenReturn(updated);

            EnterpriseUpdateVO result = enterprisesService.updateEnterpriseBasic(enterpriseId, req);

            assertNotNull(result);
            assertEquals("金融", result.industry());
        }
    }

    // ============================== updateEnterpriseContactEmail ==============================

    @Nested
    class UpdateEnterpriseContactEmail {

        private EnterpriseContactEmailUpdateReq req;

        @BeforeEach
        void setUp() {
            AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(userId).build());
            req = new EnterpriseContactEmailUpdateReq();
        }

        @Test
        void updateEnterpriseContactEmail_success() {
            mockEnterpriseBelong();
            doReturn(enterpriseUpdateWrapper).when(enterprisesService).lambdaUpdate();
            when(enterpriseUpdateWrapper.update()).thenReturn(true);

            req.setContactEmail("new@test.com");
            SecureActionContext context = SecureActionContext.builder()
                    .actionType(SecureActionType.UPDATE_ENTERPRISE_EMAIL)
                    .resourceId(enterpriseId)
                    .enterpriseId(enterpriseId)
                    .build();

            EnterpriseContactEmailUpdateVO result =
                    enterprisesService.updateEnterpriseContactEmail(
                            enterpriseId, context, req);

            assertNotNull(result);
            assertEquals(enterpriseId, result.id());
            assertEquals("new@test.com", result.contactEmail());
        }

        @Test
        void updateEnterpriseContactEmail_fail_deleteTokenCannotCrossUse() {
            mockEnterpriseBelong();
            SecureActionContext context = SecureActionContext.builder()
                    .actionType(SecureActionType.DELETE_ENTERPRISE)
                    .resourceId(enterpriseId)
                    .enterpriseId(enterpriseId)
                    .build();

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> enterprisesService.updateEnterpriseContactEmail(
                            enterpriseId, context, req));

            assertEquals(ErrorCode.SECURE_ACTION_NOT_MATCH.getCode(), exception.getCode());
        }

        @Test
        void updateEnterpriseContactEmail_fail_tokenBoundToAnotherEnterprise() {
            mockEnterpriseBelong();
            SecureActionContext context = SecureActionContext.builder()
                    .actionType(SecureActionType.UPDATE_ENTERPRISE_EMAIL)
                    .resourceId(999L)
                    .enterpriseId(999L)
                    .build();

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> enterprisesService.updateEnterpriseContactEmail(
                            enterpriseId, context, req));

            assertEquals(ErrorCode.PERMISSION_DENIED.getCode(), exception.getCode());
        }

        private void mockEnterpriseBelong() {
            doReturn(enterpriseQueryWrapper).when(enterprisesService).lambdaQuery();
            when(enterpriseQueryWrapper.exists()).thenReturn(true);
            LambdaQueryChainWrapper<EnterpriseTeamMember> memberQueryWrapper = mockQueryWrapper();
            when(memberQueryWrapper.exists()).thenReturn(true);
            when(enterpriseTeamMembersService.lambdaQuery()).thenReturn(memberQueryWrapper);
        }
    }

    // ============================== updateEnterpriseContactPhone ==============================

    @Nested
    class UpdateEnterpriseContactPhone {

        private SecureActionContext secureActionContext;

        @BeforeEach
        void setUp() {
            AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                    .userId(userId).build());
            secureActionContext = SecureActionContext.builder()
                    .actionType(SecureActionType.UPDATE_ENTERPRISE_PHONE)
                    .resourceId(enterpriseId)
                    .enterpriseId(enterpriseId)
                    .sourcePhone(contactPhone)
                    .targetPhone("13900139000")
                    .build();
        }

        @Test
        void updateEnterpriseContactPhone_success() {
            mockEnterpriseBelong();
            when(enterpriseQueryWrapper.one()).thenReturn(
                    Enterprise.builder().id(enterpriseId).contactPhone(contactPhone).build());
            doReturn(enterpriseUpdateWrapper).when(enterprisesService).lambdaUpdate();
            when(enterpriseUpdateWrapper.update()).thenReturn(true);

            EnterpriseContactPhoneUpdateVO result =
                    enterprisesService.updateEnterpriseContactPhone(
                            enterpriseId, secureActionContext);

            assertNotNull(result);
            assertEquals(enterpriseId, result.id());
            assertEquals("13900139000", result.contactPhone());
        }

        @Test
        void updateEnterpriseContactPhone_fail_actionTypeMismatch() {
            mockEnterpriseBelong();
            secureActionContext.setActionType(SecureActionType.UPDATE_ENTERPRISE_EMAIL);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> enterprisesService.updateEnterpriseContactPhone(
                            enterpriseId, secureActionContext));

            assertEquals(ErrorCode.SECURE_ACTION_NOT_MATCH.getCode(), ex.getCode());
        }

        @Test
        void updateEnterpriseContactPhone_fail_oldPhoneChangedAfterVerification() {
            mockEnterpriseBelong();
            when(enterpriseQueryWrapper.one()).thenReturn(
                    Enterprise.builder()
                            .id(enterpriseId)
                            .contactPhone("13700000000")
                            .build());

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> enterprisesService.updateEnterpriseContactPhone(
                            enterpriseId, secureActionContext));

            assertEquals(ErrorCode.ENTERPRISE_CONTACT_CHANGED.getCode(), ex.getCode());
            verify(enterpriseUpdateWrapper, never()).update();
        }

        private void mockEnterpriseBelong() {
            doReturn(enterpriseQueryWrapper).when(enterprisesService).lambdaQuery();
            when(enterpriseQueryWrapper.exists()).thenReturn(true);
            LambdaQueryChainWrapper<EnterpriseTeamMember> memberQueryWrapper = mockQueryWrapper();
            when(memberQueryWrapper.exists()).thenReturn(true);
            when(enterpriseTeamMembersService.lambdaQuery()).thenReturn(memberQueryWrapper);
        }
    }

    // ============================== deleteEnterprise ==============================

    @Nested
    class DeleteEnterprise {

        @BeforeEach
        void setUp() {
            AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(userId).build());
        }

        @Test
        void deleteEnterprise_success() {
            doReturn(enterpriseQueryWrapper).when(enterprisesService).lambdaQuery();
            when(enterpriseQueryWrapper.exists()).thenReturn(true);
            LambdaQueryChainWrapper<EnterpriseTeamMember> memberQueryWrapper = mockQueryWrapper();
            when(memberQueryWrapper.exists()).thenReturn(true);
            when(enterpriseTeamMembersService.lambdaQuery()).thenReturn(memberQueryWrapper);

            doReturn(enterpriseUpdateWrapper).when(enterprisesService).lambdaUpdate();
            when(enterpriseUpdateWrapper.update()).thenReturn(true);

            LambdaUpdateChainWrapper<EnterpriseTeamMember> memberUpdateWrapper = mockUpdateWrapper();
            when(memberUpdateWrapper.update()).thenReturn(true);
            when(enterpriseTeamMembersService.lambdaUpdate()).thenReturn(memberUpdateWrapper);

            SecureActionContext context = SecureActionContext.builder()
                    .actionType(SecureActionType.DELETE_ENTERPRISE)
                    .resourceId(enterpriseId)
                    .enterpriseId(enterpriseId)
                    .build();

            assertDoesNotThrow(() -> enterprisesService.deleteEnterprise(
                    enterpriseId, context));
            verify(enterprisesService).lambdaUpdate();
            verify(enterpriseUpdateWrapper).eq(any(), eq(enterpriseId));
            verify(enterpriseUpdateWrapper).set(any(), eq(true));
            verify(enterpriseUpdateWrapper).update();
            verify(enterpriseTeamMembersService).lambdaUpdate();
            verify(memberUpdateWrapper).eq(any(), eq(enterpriseId));
            verify(memberUpdateWrapper).set(any(), eq(true));
            verify(memberUpdateWrapper).update();
        }
    }

    // ============================== verifyEnterpriseId ==============================

    @Nested
    class VerifyEnterpriseId {

        @Test
        void verifyEnterpriseId_fail_notFound() {
            doReturn(enterpriseQueryWrapper).when(enterprisesService).lambdaQuery();
            when(enterpriseQueryWrapper.exists()).thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class,
                () -> enterprisesService.verifyEnterpriseId(enterpriseId));
            assertEquals(ErrorCode.ENTERPRISE_NOT_FOUND.getCode(), ex.getCode());
        }
    }
}
