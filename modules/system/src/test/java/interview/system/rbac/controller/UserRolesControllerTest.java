package interview.system.rbac.controller;

import interview.common.constant.Result;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.system.rbac.model.req.AssignUserRolesReq;
import interview.system.rbac.model.vo.UserRoleItemVO;
import interview.system.rbac.service.UserRolesService;
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
class UserRolesControllerTest {

    @Mock
    private UserRolesService userRolesService;

    private UserRolesController controller;

    @BeforeEach
    void setUp() {
        controller = new UserRolesController(userRolesService);
    }

    @Nested
    class AssignUserRoles {

        @Test
        void assignUserRoles_success() {
            AssignUserRolesReq req = new AssignUserRolesReq();
            req.setRoleIds(List.of(1, 2));

            Result<Void> result = controller.assignUserRoles(1L, req);

            assertNotNull(result);
            verify(userRolesService).assignUserRoles(1L, req);
        }
    }

    @Nested
    class GetUserRoles {

        @Test
        void getUserRoles_success() {
            UserRoleItemVO vo1 = UserRoleItemVO.builder()
                    .id(1L).roleCode("PLATFORM_ADMIN").roleName("平台管理员").roleScope("PLATFORM").build();
            UserRoleItemVO vo2 = UserRoleItemVO.builder()
                    .id(2L).roleCode("HR_MANAGER").roleName("HR管理员").roleScope("ENTERPRISE").build();
            when(userRolesService.getUserRoles(1L)).thenReturn(List.of(vo1, vo2));

            Result<List<UserRoleItemVO>> result = controller.getUserRoles(1L);

            assertEquals(2, result.getData().size());
            assertEquals("PLATFORM_ADMIN", result.getData().get(0).roleCode());
            verify(userRolesService).getUserRoles(1L);
        }

        @Test
        void getUserRoles_empty() {
            when(userRolesService.getUserRoles(1L)).thenReturn(List.of());

            Result<List<UserRoleItemVO>> result = controller.getUserRoles(1L);

            assertTrue(result.getData().isEmpty());
        }
    }

    @Nested
    class RemoveUserRoles {

        @Test
        void removeUserRole_success() {
            Result<Void> result = controller.removeUserRole(1L, 2);

            assertNotNull(result);
            verify(userRolesService).removeUserRole(1L, 2);
        }
    }

    @Nested
    class AssignUserRolesFailure {

        @Test
        void assignUserRoles_fail_serviceThrows() {
            AssignUserRolesReq req = new AssignUserRolesReq();
            doThrow(new BusinessException(ErrorCode.SYSTEM_ERROR))
                .when(userRolesService).assignUserRoles(anyLong(), any());

            assertThrows(BusinessException.class, () -> controller.assignUserRoles(1L, req));
        }
    }
}
