package interview.system.tenant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.common.constant.Result;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.system.tenant.model.req.TeamMemberCreateReq;
import interview.system.tenant.model.req.TeamMemberUpdateReq;
import interview.system.tenant.model.vo.TeamMemberCreateVO;
import interview.system.tenant.model.vo.TeamMemberItemVO;
import interview.system.tenant.service.EnterpriseTeamMembersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnterpriseTeamMembersControllerTest {

    @Mock
    private EnterpriseTeamMembersService teamMembersService;

    private EnterpriseTeamMembersController controller;

    @BeforeEach
    void setUp() {
        controller = new EnterpriseTeamMembersController(teamMembersService);
    }

    @Nested
    class ListTeamMembers {

        @Test
        void listTeamMembers_success() {
            TeamMemberItemVO vo = TeamMemberItemVO.builder()
                    .id(1L).userId(10L).username("member1").roleCode("HR_MANAGER").build();
            IPage<TeamMemberItemVO> page = new Page<>(1, 20, 1);
            page.setRecords(List.of(vo));
            when(teamMembersService.listTeamMembers(1L, 1, 20, "desc")).thenReturn(page);

            Result<IPage<TeamMemberItemVO>> result =
                    controller.listTeamMembers(1L, 1, 20, "createdAt", "desc");

            assertEquals(1, result.getData().getRecords().size());
            assertEquals("HR_MANAGER", result.getData().getRecords().get(0).roleCode());
            verify(teamMembersService).listTeamMembers(1L, 1, 20, "desc");
        }
    }

    @Nested
    class InviteMember {

        @Test
        void inviteMember_success() {
            TeamMemberCreateReq req = new TeamMemberCreateReq();
            req.setUserId(10L);
            req.setRoleId(2);
            TeamMemberCreateVO vo = new TeamMemberCreateVO(100L);
            when(teamMembersService.inviteMember(1L, req)).thenReturn(vo);

            Result<TeamMemberCreateVO> result = controller.inviteMember(1L, req);

            assertEquals(100L, result.getData().id());
            verify(teamMembersService).inviteMember(1L, req);
        }
    }

    @Nested
    class UpdateMemberRole {

        @Test
        void updateMemberRole_success() {
            TeamMemberUpdateReq req = new TeamMemberUpdateReq();
            req.setRoleId(3);

            Result<Void> result = controller.updateMemberRole(1L, 100L, req);

            assertNotNull(result);
            verify(teamMembersService).updateMemberRole(1L, 100L, req);
        }
    }

    @Nested
    class RemoveMember {

        @Test
        void removeMember_success() {
            Result<Void> result = controller.removeMember(1L, 100L);

            assertNotNull(result);
            verify(teamMembersService).removeMember(1L, 100L);
        }
    }

    @Nested
    class InviteMemberFailure {

        @Test
        void inviteMember_fail_serviceThrows() {
            TeamMemberCreateReq req = new TeamMemberCreateReq();
            when(teamMembersService.inviteMember(anyLong(), any()))
                .thenThrow(new BusinessException(ErrorCode.ENTERPRISE_NOT_FOUND));

            assertThrows(BusinessException.class, () -> controller.inviteMember(1L, req));
        }
    }
}
