package interview.system.rbac.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.common.constant.Result;
import interview.common.enums.ErrorCode;
import interview.common.enums.UserType;
import interview.common.exception.BusinessException;
import interview.system.auth.model.entity.User;
import interview.system.auth.model.enums.UserStatus;
import interview.system.auth.service.UsersService;
import interview.system.rbac.model.req.AdminUserSearchReq;
import interview.system.rbac.model.vo.AdminUserDetailVO;
import interview.system.rbac.model.vo.AdminUserListItemVO;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsersControllerTest {

    @Mock
    private UsersService usersService;
    @Mock
    private UserRolesService userRolesService;

    private UsersController controller;

    @BeforeEach
    void setUp() {
        controller = new UsersController(usersService, userRolesService);
    }

    @Nested
    class PageUsers {

        @Test
        void pageUsers_success() {
            AdminUserSearchReq req = new AdminUserSearchReq();
            Page<AdminUserListItemVO> page = new Page<>(1, 20, 1);
            page.setRecords(List.of(AdminUserListItemVO.builder()
                    .userId(1L).username("zhuxi").nickname("朱某")
                    .phone("138****8000").userType("CANDIDATE").status("NORMAL")
                    .build()));
            when(usersService.pageUsers(req)).thenReturn(page);

            Result<IPage<AdminUserListItemVO>> result = controller.pageUsers(req);

            assertEquals(1, result.getData().getTotal());
            assertEquals("zhuxi", result.getData().getRecords().get(0).username());
            verify(usersService).pageUsers(req);
        }
    }

    @Nested
    class GetUserDetail {

        @Test
        void getUserDetail_success() {
            User user = User.builder()
                    .id(1L).username("zhuxi").email("zhuxi@example.com").nickname("朱某")
                    .phone("13800000000").userType(UserType.CANDIDATE)
                    .status(UserStatus.NORMAL).build();
            when(usersService.getById(1L)).thenReturn(user);
            when(userRolesService.getUserRoles(1L)).thenReturn(List.of(
                    UserRoleItemVO.builder()
                            .id(1L).roleCode("PLATFORM_OPS").roleName("平台运维")
                            .roleScope("PLATFORM").build()));

            Result<AdminUserDetailVO> result = controller.getUserDetail(1L);

            assertEquals("zhuxi", result.getData().username());
            assertEquals("CANDIDATE", result.getData().userType());
            assertEquals(1, result.getData().platformRoles().size());
            assertEquals("PLATFORM_OPS", result.getData().platformRoles().get(0).roleCode());
        }

        @Test
        void getUserDetail_notFound() {
            when(usersService.getById(404L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> controller.getUserDetail(404L));

            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
            verify(userRolesService, never()).getUserRoles(anyLong());
        }
    }
}
