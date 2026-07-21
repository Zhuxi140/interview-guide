package interview.system.tenant;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.system.rbac.model.entity.Role;
import interview.system.rbac.service.RolesService;
import interview.system.tenant.mapper.EnterpriseTeamMembersMapper;
import interview.system.tenant.model.bo.TeamMemberItemBO;
import interview.system.tenant.model.entity.EnterpriseTeamMember;
import interview.system.tenant.model.req.TeamMemberCreateReq;
import interview.system.tenant.model.req.TeamMemberUpdateReq;
import interview.system.tenant.model.vo.TeamMemberCreateVO;
import interview.system.tenant.model.vo.TeamMemberItemVO;
import interview.system.tenant.model.entity.Enterprise;
import interview.system.tenant.service.EnterpriseTeamMembersServiceImpl;
import interview.system.tenant.service.EnterprisesService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;
import java.util.List;

import static interview.system.TestMockUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnterpriseTeamMembersServiceImplTest {

    @Mock private EnterprisesService enterprisesService;
    @Mock private EnterpriseTeamMembersMapper enterpriseTeamMembersMapper;
    @Mock private RolesService rolesService;

    private EnterpriseTeamMembersServiceImpl teamMembersService;
    private LambdaQueryChainWrapper<EnterpriseTeamMember> memberQueryWrapper;
    private LambdaUpdateChainWrapper<EnterpriseTeamMember> memberUpdateWrapper;

    private final Long userId = 1L;
    private final Long enterpriseId = 10001L;
    private final Long memberId = 50001L;
    private final Integer hrManagerRoleId = interview.common.enums.Role.HR_MANAGER.getCode();
    private final Integer ownerRoleId = interview.common.enums.Role.ENTERPRISE_OWNER.getCode();
    private final Integer adminRoleId = interview.common.enums.Role.ENTERPRISE_ADMIN.getCode();

    @BeforeEach
    void setUp() {
        teamMembersService = spy(new EnterpriseTeamMembersServiceImpl(
            enterprisesService, enterpriseTeamMembersMapper, rolesService
        ));
        memberQueryWrapper = mockQueryWrapper();
        memberUpdateWrapper = mockUpdateWrapper();
    }

    @AfterEach
    void tearDown() {
        AuthContext.remove();
    }

    // ============================== listTeamMembers ==============================

    @Nested
    class ListTeamMembers {

        private LambdaQueryChainWrapper<Enterprise> enterpriseQueryWrapper;

        @BeforeEach
        void setUp() {
            AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(userId).build());
            enterpriseQueryWrapper = mockQueryWrapper();
            when(enterprisesService.lambdaQuery()).thenReturn(enterpriseQueryWrapper);
            when(enterpriseQueryWrapper.exists()).thenReturn(true);
            doReturn(memberQueryWrapper).when(teamMembersService).lambdaQuery();
            when(memberQueryWrapper.exists()).thenReturn(true);
        }

        @Test
        void listTeamMembers_success_withRoleCodeMapping() {

            TeamMemberItemBO bo1 = TeamMemberItemBO.builder()
                .id(memberId).userId(10L).username("user1").nickname("用户1")
                .email("user1@test.com").roleId(hrManagerRoleId)
                .createdAt(OffsetDateTime.now()).build();
            TeamMemberItemBO bo2 = TeamMemberItemBO.builder()
                .id(memberId + 1).userId(11L).username("user2").nickname("用户2")
                .email("user2@test.com").roleId(adminRoleId)
                .createdAt(OffsetDateTime.now()).build();
            Page<TeamMemberItemBO> boPage = new Page<>(1, 20);
            boPage.setRecords(List.of(bo1, bo2));
            boPage.setTotal(2);
            when(enterpriseTeamMembersMapper.getTeamMembersPage(any(Page.class), eq(enterpriseId))).thenReturn(boPage);

            Role role1 = new Role();
            role1.setId(hrManagerRoleId);
            role1.setRoleCode("HR_MANAGER");
            Role role2 = new Role();
            role2.setId(adminRoleId);
            role2.setRoleCode("ENTERPRISE_ADMIN");
            LambdaQueryChainWrapper<Role> roleQueryWrapper = mockQueryWrapper();
            when(roleQueryWrapper.list()).thenReturn(List.of(role1, role2));
            when(rolesService.lambdaQuery()).thenReturn(roleQueryWrapper);

            IPage<TeamMemberItemVO> result = teamMembersService.listTeamMembers(enterpriseId, 1, 20, "DESC");

            assertEquals(2, result.getRecords().size());
            assertEquals("HR_MANAGER", result.getRecords().get(0).roleCode());
            assertEquals("ENTERPRISE_ADMIN", result.getRecords().get(1).roleCode());
        }

        @Test
        void listTeamMembers_success_emptyRecords() {
            Page<TeamMemberItemBO> boPage = new Page<>(1, 20);
            boPage.setRecords(List.of());
            boPage.setTotal(0);
            when(enterpriseTeamMembersMapper.getTeamMembersPage(any(Page.class), eq(enterpriseId))).thenReturn(boPage);

            IPage<TeamMemberItemVO> result = teamMembersService.listTeamMembers(enterpriseId, 1, 20, "DESC");

            assertTrue(result.getRecords().isEmpty());
        }

        @Test
        void listTeamMembers_success_ascOrder() {
            TeamMemberItemBO bo = TeamMemberItemBO.builder()
                .id(memberId).userId(10L).username("user1").nickname("用户1")
                .email("user1@test.com").roleId(hrManagerRoleId)
                .createdAt(OffsetDateTime.now()).build();
            Page<TeamMemberItemBO> boPage = new Page<>(1, 20);
            boPage.setRecords(List.of(bo));
            boPage.setTotal(1);
            when(enterpriseTeamMembersMapper.getTeamMembersPage(any(Page.class), eq(enterpriseId))).thenReturn(boPage);

            Role role = new Role();
            role.setId(hrManagerRoleId);
            role.setRoleCode("HR_MANAGER");
            LambdaQueryChainWrapper<Role> roleQueryWrapper = mockQueryWrapper();
            when(roleQueryWrapper.list()).thenReturn(List.of(role));
            when(rolesService.lambdaQuery()).thenReturn(roleQueryWrapper);

            IPage<TeamMemberItemVO> result = teamMembersService.listTeamMembers(enterpriseId, 1, 20, "ASC");

            assertEquals(1, result.getRecords().size());
            assertEquals("HR_MANAGER", result.getRecords().get(0).roleCode());
        }
    }

    // ============================== inviteMember ==============================

    @Nested
    class InviteMember {

        private TeamMemberCreateReq req;
        private final Long inviteUserId = 99L;

        @BeforeEach
        void setUp() {
            AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(userId).build());
            req = new TeamMemberCreateReq();
            req.setUserId(inviteUserId);
            req.setRoleId(hrManagerRoleId);
        }

        @Test
        void inviteMember_success() {
            doNothing().when(teamMembersService).verifyEnterpriseId(enterpriseId);
            doNothing().when(teamMembersService).verifyRoleId(hrManagerRoleId);

            doReturn(memberQueryWrapper).when(teamMembersService).lambdaQuery();
            when(memberQueryWrapper.exists()).thenReturn(false);
            doAnswer(invocation -> {
                EnterpriseTeamMember m = invocation.getArgument(0);
                m.setId(memberId);
                return true;
            }).when(teamMembersService).save(any(EnterpriseTeamMember.class));

            TeamMemberCreateVO result = teamMembersService.inviteMember(enterpriseId, req);

            assertNotNull(result);
            assertEquals(memberId, result.id());
        }

        @Test
        void inviteMember_fail_enterpriseNotFound() {
            doThrow(new BusinessException(ErrorCode.ENTERPRISE_NOT_FOUND))
                .when(teamMembersService).verifyEnterpriseId(enterpriseId);

            BusinessException ex = assertThrows(BusinessException.class,
                () -> teamMembersService.inviteMember(enterpriseId, req));
            assertEquals(ErrorCode.ENTERPRISE_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        void inviteMember_fail_roleNotEnterpriseScope() {
            doNothing().when(teamMembersService).verifyEnterpriseId(enterpriseId);
            doThrow(new BusinessException(ErrorCode.NO_ALLOW_ROLE))
                .when(teamMembersService).verifyRoleId(hrManagerRoleId);

            BusinessException ex = assertThrows(BusinessException.class,
                () -> teamMembersService.inviteMember(enterpriseId, req));
            assertEquals(ErrorCode.NO_ALLOW_ROLE.getCode(), ex.getCode());
        }

        @Test
        void inviteMember_fail_memberAlreadyExists() {
            doNothing().when(teamMembersService).verifyEnterpriseId(enterpriseId);
            doNothing().when(teamMembersService).verifyRoleId(hrManagerRoleId);

            doReturn(memberQueryWrapper).when(teamMembersService).lambdaQuery();
            when(memberQueryWrapper.exists()).thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class,
                () -> teamMembersService.inviteMember(enterpriseId, req));
            assertEquals(ErrorCode.MEMBER_ALREADY_EXISTS.getCode(), ex.getCode());
        }

        @Test
        void inviteMember_fail_dataIntegrityViolation() {
            doNothing().when(teamMembersService).verifyEnterpriseId(enterpriseId);
            doNothing().when(teamMembersService).verifyRoleId(hrManagerRoleId);

            doReturn(memberQueryWrapper).when(teamMembersService).lambdaQuery();
            when(memberQueryWrapper.exists()).thenReturn(false);
            doThrow(DataIntegrityViolationException.class).when(teamMembersService).save(any(EnterpriseTeamMember.class));

            BusinessException ex = assertThrows(BusinessException.class,
                () -> teamMembersService.inviteMember(enterpriseId, req));
            assertEquals(ErrorCode.MEMBER_ALREADY_EXISTS.getCode(), ex.getCode());
        }
    }

    // ============================== updateMemberRole ==============================

    @Nested
    class UpdateMemberRole {

        private TeamMemberUpdateReq req;

        @BeforeEach
        void setUp() {
            AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                    .userId(userId)
                    .build());
            req = new TeamMemberUpdateReq();
            req.setRoleId(hrManagerRoleId);
        }

        @Test
        void updateMemberRole_success() {
            doNothing().when(teamMembersService).verifyEnterpriseId(enterpriseId);
            doNothing().when(teamMembersService).verifyMemberIdBelong(enterpriseId, memberId);
            doNothing().when(teamMembersService).verifyRoleId(hrManagerRoleId);

            doReturn(memberUpdateWrapper).when(teamMembersService).lambdaUpdate();
            when(memberUpdateWrapper.update()).thenReturn(true);

            assertDoesNotThrow(() -> teamMembersService.updateMemberRole(enterpriseId, memberId, req));
        }

        @Test
        void updateMemberRole_fail_cannotOperateOwner() {
            doNothing().when(teamMembersService).verifyEnterpriseId(enterpriseId);
            doNothing().when(teamMembersService).verifyMemberIdBelong(enterpriseId, memberId);
            req.setRoleId(ownerRoleId);

            BusinessException ex = assertThrows(BusinessException.class,
                () -> teamMembersService.updateMemberRole(enterpriseId, memberId, req));
            assertEquals(ErrorCode.NO_OPERATE_ENTERPRISE_OWNER.getCode(), ex.getCode());
        }

        @Test
        void updateMemberRole_fail_roleNotEnterprise() {
            doNothing().when(teamMembersService).verifyEnterpriseId(enterpriseId);
            doNothing().when(teamMembersService).verifyMemberIdBelong(enterpriseId, memberId);
            doThrow(new BusinessException(ErrorCode.NO_ALLOW_ROLE))
                .when(teamMembersService).verifyRoleId(hrManagerRoleId);

            BusinessException ex = assertThrows(BusinessException.class,
                () -> teamMembersService.updateMemberRole(enterpriseId, memberId, req));
            assertEquals(ErrorCode.NO_ALLOW_ROLE.getCode(), ex.getCode());
        }

        @Test
        void updateMemberRole_fail_memberNotFound() {
            doNothing().when(teamMembersService).verifyEnterpriseId(enterpriseId);
            doThrow(new BusinessException(ErrorCode.ENTERPRISE_MEMBER_NOT_FOUND))
                .when(teamMembersService).verifyMemberIdBelong(enterpriseId, memberId);

            BusinessException ex = assertThrows(BusinessException.class,
                () -> teamMembersService.updateMemberRole(enterpriseId, memberId, req));
            assertEquals(ErrorCode.ENTERPRISE_MEMBER_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        void updateMemberRole_fail_memberNotBelong() {
            doNothing().when(teamMembersService).verifyEnterpriseId(enterpriseId);
            doThrow(new BusinessException(ErrorCode.ENTERPRISE_NOT_BELONG))
                .when(teamMembersService).verifyMemberIdBelong(enterpriseId, memberId);

            BusinessException ex = assertThrows(BusinessException.class,
                () -> teamMembersService.updateMemberRole(enterpriseId, memberId, req));
            assertEquals(ErrorCode.ENTERPRISE_NOT_BELONG.getCode(), ex.getCode());
        }
    }

    // ============================== removeMember ==============================

    @Nested
    class RemoveMember {

        private final Long removeUserId = 50L;

        @Test
        void removeMember_success() {
            doNothing().when(teamMembersService).verifyEnterpriseId(enterpriseId);

            doReturn(memberQueryWrapper).when(teamMembersService).lambdaQuery();
            EnterpriseTeamMember member = EnterpriseTeamMember.builder()
                .enterpriseId(enterpriseId).roleId(hrManagerRoleId).userId(removeUserId)
                .build();
            when(memberQueryWrapper.one()).thenReturn(member);

            AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(userId).build());

            doReturn(memberUpdateWrapper).when(teamMembersService).lambdaUpdate();
            when(memberUpdateWrapper.update()).thenReturn(true);

            assertDoesNotThrow(() -> teamMembersService.removeMember(enterpriseId, memberId));
        }

        @Test
        void removeMember_fail_memberNotFound() {
            doNothing().when(teamMembersService).verifyEnterpriseId(enterpriseId);

            doReturn(memberQueryWrapper).when(teamMembersService).lambdaQuery();
            when(memberQueryWrapper.one()).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                () -> teamMembersService.removeMember(enterpriseId, memberId));
            assertEquals(ErrorCode.ENTERPRISE_MEMBER_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        void removeMember_fail_cannotRemoveOwner() {
            doNothing().when(teamMembersService).verifyEnterpriseId(enterpriseId);

            doReturn(memberQueryWrapper).when(teamMembersService).lambdaQuery();
            EnterpriseTeamMember owner = EnterpriseTeamMember.builder()
                .enterpriseId(enterpriseId).roleId(ownerRoleId).userId(removeUserId)
                .build();
            when(memberQueryWrapper.one()).thenReturn(owner);

            BusinessException ex = assertThrows(BusinessException.class,
                () -> teamMembersService.removeMember(enterpriseId, memberId));
            assertEquals(ErrorCode.NO_OPERATE_ENTERPRISE_OWNER.getCode(), ex.getCode());
        }

        @Test
        void removeMember_fail_cannotRemoveSelf() {
            doNothing().when(teamMembersService).verifyEnterpriseId(enterpriseId);

            doReturn(memberQueryWrapper).when(teamMembersService).lambdaQuery();
            EnterpriseTeamMember self = EnterpriseTeamMember.builder()
                .enterpriseId(enterpriseId).roleId(hrManagerRoleId).userId(userId)
                .build();
            when(memberQueryWrapper.one()).thenReturn(self);

            AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(userId).build());

            BusinessException ex = assertThrows(BusinessException.class,
                () -> teamMembersService.removeMember(enterpriseId, memberId));
            assertEquals(ErrorCode.NO_DELETE_OWNER.getCode(), ex.getCode());
        }
    }
}
