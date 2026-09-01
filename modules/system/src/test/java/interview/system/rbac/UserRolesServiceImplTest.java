package interview.system.rbac;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.system.auth.model.entity.User;
import interview.system.auth.service.UsersServiceImpl;
import interview.system.rbac.mapper.UserRolesMapper;
import interview.system.rbac.model.entity.Role;
import interview.system.rbac.model.entity.UserRole;
import interview.system.rbac.model.req.AssignUserRolesReq;
import interview.system.rbac.model.vo.UserRoleItemVO;
import interview.system.rbac.service.RolesServiceImpl;
import interview.system.rbac.service.UserRolesServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static interview.system.TestMockUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRolesServiceImplTest {

    @Mock
    private UserRolesMapper userRolesMapper;
    @Mock
    private RolesServiceImpl rolesService;
    @Mock
    private UsersServiceImpl usersService;
    private UserRolesServiceImpl userRolesService;
    private LambdaQueryChainWrapper<UserRole> queryWrapper;
    private LambdaUpdateChainWrapper<UserRole> updateWrapper;

    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        userRolesService = spy(new UserRolesServiceImpl(userRolesMapper, rolesService, usersService));
        queryWrapper = mockQueryWrapper();
        updateWrapper = mockUpdateWrapper();
    }

    @Nested
    class AssignUserRoles {

        private AssignUserRolesReq req;

        @BeforeEach
        void setUp() {
            req = new AssignUserRolesReq();
            doReturn(queryWrapper).when(userRolesService).lambdaQuery();

            LambdaQueryChainWrapper<User> userCheckQ = mockQueryWrapper();
            when(userCheckQ.exists()).thenReturn(true);
            when(usersService.lambdaQuery()).thenReturn(userCheckQ);

            LambdaQueryChainWrapper<Role> roleCheckQ = mockQueryWrapper();
            when(roleCheckQ.count()).thenAnswer(invocation ->
                    req.getRoleIds().stream().distinct().count());
            when(rolesService.lambdaQuery()).thenReturn(roleCheckQ);
        }

        /**
         * 测试对象：UserRolesServiceImpl.assignUserRoles(Long userId, AssignUserRolesReq req)
         * 测试功能：为用户分配 3 个新角色（用户当前无任何角色），应全部插入
         * 输入：userId=1L, req.roleIds=[1, 2, 3]
         *       lambdaQuery().eq(..., 1L).select(...).list() 返回空（无已有角色）
         *       saveBatch(anyList()) 返回 true
         * 预期输出：saveBatch 被调用 1 次，传入 3 条 SysUserRole（userId 均=1L）
         * 可能异常：BusinessException（Transactional rollback 时）
         */
        @Test
        void assignUserRoles_success() {
            req.setRoleIds(List.of(1, 2, 3));
            when(queryWrapper.list()).thenReturn(List.of());
            doReturn(true).when(userRolesService).saveBatch(anyList());

            userRolesService.assignUserRoles(userId, req);

            verify(userRolesService).saveBatch(argThat(list -> {
                List<UserRole> roles = cast(list);
                return roles.size() == 3
                        && roles.stream().allMatch(r -> r.getUserId().equals(userId));
            }));
        }

        /**
         * 测试对象：UserRolesServiceImpl.assignUserRoles(Long userId, AssignUserRolesReq req)
         * 测试功能：请求中存在重复 roleId（如 [1, 2, 1, 2]），应去重后只插入 2 条
         * 输入：userId=1L, req.roleIds=[1, 2, 1, 2]
         *       lambdaQuery().list() 返回空
         * 预期输出：saveBatch 被调用 1 次，传入 2 条 SysUserRole（roleId={1, 2}）
         * 可能异常：BusinessException
         */
        @Test
        void assignUserRoles_duplicateInRequest_deduplicates() {
            req.setRoleIds(List.of(1, 2, 1, 2));
            when(queryWrapper.list()).thenReturn(List.of());
            doReturn(true).when(userRolesService).saveBatch(anyList());

            userRolesService.assignUserRoles(userId, req);

            verify(userRolesService).saveBatch(argThat(list -> {
                List<UserRole> roles = cast(list);
                return roles.size() == 2;
            }));
        }

        /**
         * 测试对象：UserRolesServiceImpl.assignUserRoles(Long userId, AssignUserRolesReq req)
         * 测试功能：请求的所有角色用户已拥有，应抛出 ROLE_ALREADY_ASSIGNED 异常
         * 输入：userId=1L, req.roleIds=[1, 2]
         *       lambdaQuery().list() 返回 2 条已有 SysUserRole（roleId={1, 2}）
         * 预期输出：BusinessException(ErrorCode.ROLE_ALREADY_ASSIGNED)
         * 可能异常：BusinessException
         */
        @Test
        void assignUserRoles_allAlreadyExist_throwException() {
            req.setRoleIds(List.of(1, 2));
            UserRole existing1 = UserRole.builder().userId(userId).roleId(1).build();
            UserRole existing2 = UserRole.builder().userId(userId).roleId(2).build();
            when(queryWrapper.list()).thenReturn(List.of(existing1, existing2));

            BusinessException ex = assertThrows(BusinessException.class,
                () -> userRolesService.assignUserRoles(userId, req));
            assertEquals(ErrorCode.ROLE_ALREADY_ASSIGNED.getCode(), ex.getCode());
        }

        /**
         * 测试对象：UserRolesServiceImpl.assignUserRoles(Long userId, AssignUserRolesReq req)
         * 测试功能：请求中部分角色已存在（roleId=1），只插入不存在的部分（roleId=2, 3）
         * 输入：userId=1L, req.roleIds=[1, 2, 3]
         *       lambdaQuery().list() 返回 1 条已有 SysUserRole（roleId=1）
         * 预期输出：saveBatch 传入 2 条 SysUserRole（roleId={2, 3}），不包含 roleId=1
         * 可能异常：BusinessException
         */
        @Test
        void assignUserRoles_partialExisting_partialNew() {
            req.setRoleIds(List.of(1, 2, 3));
            UserRole existing1 = UserRole.builder().userId(userId).roleId(1).build();
            when(queryWrapper.list()).thenReturn(List.of(existing1));
            doReturn(true).when(userRolesService).saveBatch(anyList());

            userRolesService.assignUserRoles(userId, req);

            verify(userRolesService).saveBatch(argThat(list -> {
                List<UserRole> roles = cast(list);
                return roles.size() == 2
                        && roles.stream().noneMatch(r -> r.getRoleId().equals(1))
                        && roles.stream().allMatch(r -> r.getUserId().equals(userId));
            }));
        }
    }

    @Nested
    class GetUserRoles {

        /**
         * 测试对象：UserRolesServiceImpl.getUserRoles(Long userId)
         * 测试功能：查询用户已分配的角色列表，验证 Mapper XML LEFT JOIN 结果映射
         * 输入：userId=1L
         *       userRolesMapper.getUserRoles(1L) 返回 2 条 UserRoleItemVO
         *       (PLATFORM_ADMIN/PLATFORM, HR_MANAGER/ENTERPRISE)
         * 预期输出：返回 List<UserRoleItemVO> size=2
         *         第 0 条 roleCode=PLATFORM_ADMIN
         *         第 1 条 roleCode=HR_MANAGER
         * 可能异常：N/A
         */
        @Test
        void getUserRoles_success() {
            UserRoleItemVO role1 = UserRoleItemVO.builder()
                    .id(1L).roleCode("PLATFORM_ADMIN").roleName("平台管理员").roleScope("PLATFORM").build();
            UserRoleItemVO role2 = UserRoleItemVO.builder()
                    .id(2L).roleCode("HR_MANAGER").roleName("HR管理员").roleScope("ENTERPRISE").build();
            when(userRolesMapper.getUserRoles(userId)).thenReturn(List.of(role1, role2));

            List<UserRoleItemVO> result = userRolesService.getUserRoles(userId);

            assertEquals(2, result.size());
            assertEquals("PLATFORM_ADMIN", result.get(0).roleCode());
            assertEquals("HR_MANAGER", result.get(1).roleCode());
        }

        /**
         * 测试对象：UserRolesServiceImpl.getUserRoles(Long userId)
         * 测试功能：用户没有任何角色时返回空列表
         * 输入：userId=1L
         *       userRolesMapper.getUserRoles(1L) 返回空 List
         * 预期输出：返回空 List<UserRoleItemVO>，isEmpty() == true
         * 可能异常：N/A
         */
        @Test
        void getUserRoles_empty() {
            when(userRolesMapper.getUserRoles(userId)).thenReturn(List.of());

            List<UserRoleItemVO> result = userRolesService.getUserRoles(userId);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class RemoveUserRoles {

        @BeforeEach
        void setUp() {
            doReturn(updateWrapper).when(userRolesService).lambdaUpdate();
        }

        /**
         * 测试对象：UserRolesServiceImpl.removeUserRole(Long userId, Integer roleId)
         * 测试功能：精确删除指定用户的单个平台角色
         * 输入：userId=1L, roleId=2
         * 预期输出：lambdaUpdate 执行两个等值条件并删除成功
         * 可能异常：N/A
         */
        @Test
        void removeUserRole_success() {
            when(updateWrapper.remove()).thenReturn(true);

            userRolesService.removeUserRole(userId, 2);

            verify(updateWrapper).eq(any(), eq(userId));
            verify(updateWrapper).eq(any(), eq(2));
            verify(updateWrapper).remove();
        }

        /**
         * 测试对象：UserRolesServiceImpl.removeUserRole(Long userId, Integer roleId)
         * 测试功能：角色关联不存在时返回明确业务错误
         * 输入：userId=1L, roleId=999
         * 预期输出：抛出 BusinessException
         */
        @Test
        void removeUserRole_notExists() {
            when(updateWrapper.remove()).thenReturn(false);

            assertThrows(BusinessException.class,
                    () -> userRolesService.removeUserRole(userId, 999));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> cast(Object obj) {
        return (List<T>) obj;
    }
}
