package interview.system.rbac.controller;

import interview.common.constant.Result;
import interview.system.rbac.model.vo.RoleDetailVO;
import interview.system.rbac.model.vo.RoleListItemVO;
import interview.system.rbac.service.RolesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolesControllerTest {

    @Mock
    private RolesService rolesService;

    private RolesController controller;

    @BeforeEach
    void setUp() {
        controller = new RolesController(rolesService);
    }

    @Nested
    class ListRoles {

        @Test
        void listRoles_success() {
            RoleListItemVO vo1 = RoleListItemVO.builder()
                    .id(1).roleCode("PLATFORM_ADMIN").roleName("平台管理员").roleScope("PLATFORM")
                    .createdAt(OffsetDateTime.now()).build();
            RoleListItemVO vo2 = RoleListItemVO.builder()
                    .id(2).roleCode("HR_MANAGER").roleName("HR管理员").roleScope("ENTERPRISE")
                    .createdAt(OffsetDateTime.now()).build();
            when(rolesService.listRoles()).thenReturn(List.of(vo1, vo2));

            Result<List<RoleListItemVO>> result = controller.listRoles();

            assertEquals(2, result.getData().size());
            assertEquals("PLATFORM_ADMIN", result.getData().get(0).roleCode());
            verify(rolesService).listRoles();
        }

        @Test
        void listRoles_empty() {
            when(rolesService.listRoles()).thenReturn(List.of());

            Result<List<RoleListItemVO>> result = controller.listRoles();

            assertTrue(result.getData().isEmpty());
        }
    }

    @Nested
    class GetRoleDetail {

        @Test
        void getRoleDetail_success() {
            RoleDetailVO vo = RoleDetailVO.builder()
                    .id(1L).roleCode("PLATFORM_ADMIN").roleName("平台管理员")
                    .roleScope("PLATFORM").permissions(List.of("user:read", "user:write"))
                    .createdAt(OffsetDateTime.now()).build();
            when(rolesService.getRoleDetail(1L)).thenReturn(vo);

            Result<RoleDetailVO> result = controller.getRoleDetail(1L);

            assertEquals("PLATFORM_ADMIN", result.getData().roleCode());
            assertEquals(2, result.getData().permissions().size());
            verify(rolesService).getRoleDetail(1L);
        }

        @Test
        void getRoleDetail_notFound() {
            when(rolesService.getRoleDetail(anyLong())).thenReturn(null);

            Result<RoleDetailVO> result = controller.getRoleDetail(999L);

            assertNull(result.getData());
            assertNotNull(result.getCode());
        }
    }
}
