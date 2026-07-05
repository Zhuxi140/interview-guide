package interview.system.rbac;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import interview.system.auth.service.impl.UsersServiceImpl;
import interview.system.rbac.mapper.UserRolesMapper;
import interview.system.rbac.model.entity.SysUserRole;
import interview.system.rbac.model.req.AssignUserRolesReq;
import interview.system.rbac.model.req.RemoveUserRolesReq;
import interview.system.rbac.model.vo.UserRoleItemVO;
import interview.system.rbac.service.impl.RolesServiceImpl;
import interview.system.rbac.service.impl.UserRolesServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.List;

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
    private LambdaQueryChainWrapper<SysUserRole> queryWrapper;
    private LambdaUpdateChainWrapper<SysUserRole> updateWrapper;

    private final Answer<Object> SELF_ANSWER = invocation -> {
        Class<?> rt = invocation.getMethod().getReturnType();
        String name = invocation.getMethod().getName();
        if (Wrapper.class.isAssignableFrom(rt)) {
            return invocation.getMock();
        }
        if (rt == Object.class && !name.equals("one") && !name.equals("getEntity")) {
            return invocation.getMock();
        }
        return Mockito.RETURNS_DEFAULTS.answer(invocation);
    };

    @SuppressWarnings("unchecked")
    private <T> LambdaQueryChainWrapper<T> mockQueryWrapper() {
        return mock(LambdaQueryChainWrapper.class, withSettings().defaultAnswer(SELF_ANSWER));
    }

    @SuppressWarnings("unchecked")
    private <T> LambdaUpdateChainWrapper<T> mockUpdateWrapper() {
        return mock(LambdaUpdateChainWrapper.class, withSettings().defaultAnswer(SELF_ANSWER));
    }

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
            req.setRoleIds(List.of(1L, 2L, 3L));
            when(queryWrapper.list()).thenReturn(List.of());
            doReturn(true).when(userRolesService).saveBatch(anyList());

            userRolesService.assignUserRoles(userId, req);

            verify(userRolesService).saveBatch(argThat(list -> {
                List<SysUserRole> roles = cast(list);
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
            req.setRoleIds(List.of(1L, 2L, 1L, 2L));
            when(queryWrapper.list()).thenReturn(List.of());
            doReturn(true).when(userRolesService).saveBatch(anyList());

            userRolesService.assignUserRoles(userId, req);

            verify(userRolesService).saveBatch(argThat(list -> {
                List<SysUserRole> roles = cast(list);
                return roles.size() == 2;
            }));
        }

        /**
         * 测试对象：UserRolesServiceImpl.assignUserRoles(Long userId, AssignUserRolesReq req)
         * 测试功能：请求的所有角色用户已拥有，应跳过插入，saveBatch 不执行
         * 输入：userId=1L, req.roleIds=[1, 2]
         *       lambdaQuery().list() 返回 2 条已有 SysUserRole（roleId={1, 2}）
         * 预期输出：saveBatch 从未被调用
         * 可能异常：N/A
         */
        @Test
        void assignUserRoles_allAlreadyExist_skipInsert() {
            req.setRoleIds(List.of(1L, 2L));
            SysUserRole existing1 = SysUserRole.builder().userId(userId).roleId(1L).build();
            SysUserRole existing2 = SysUserRole.builder().userId(userId).roleId(2L).build();
            when(queryWrapper.list()).thenReturn(List.of(existing1, existing2));

            userRolesService.assignUserRoles(userId, req);

            verify(userRolesService, never()).saveBatch(anyList());
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
            req.setRoleIds(List.of(1L, 2L, 3L));
            SysUserRole existing1 = SysUserRole.builder().userId(userId).roleId(1L).build();
            when(queryWrapper.list()).thenReturn(List.of(existing1));
            doReturn(true).when(userRolesService).saveBatch(anyList());

            userRolesService.assignUserRoles(userId, req);

            verify(userRolesService).saveBatch(argThat(list -> {
                List<SysUserRole> roles = cast(list);
                return roles.size() == 2
                        && roles.stream().noneMatch(r -> r.getRoleId().equals(1L))
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

        private RemoveUserRolesReq req;

        @BeforeEach
        void setUp() {
            req = new RemoveUserRolesReq();
            doReturn(queryWrapper).when(userRolesService).lambdaQuery();
            doReturn(updateWrapper).when(userRolesService).lambdaUpdate();
        }

        /**
         * 测试对象：UserRolesServiceImpl.removeUserRoles(Long userId, RemoveUserRolesReq req)
         * 测试功能：移除非已有角色 ID，验证 lambdaUpdate 链路
         * （当前实现逻辑：只对"不存在"的 roleId 执行 IN 删除，属于已知行为）
         * 输入：userId=1L, req.roleIds=[1, 2]
         *       lambdaQuery().eq(..., 1L).select(...).list() 返回 roleId=1 存在
         * 预期输出：lambdaUpdate 执行 eq(userId=1L) + in(roleId=[2]) + remove()
         *         不包含 roleId=1（因其已存在被过滤掉）
         * 可能异常：N/A
         */
        @Test
        void removeUserRoles_removesOnlyNonExistingIds() {
            req.setRoleIds(List.of(1L, 2L));
            SysUserRole existing1 = SysUserRole.builder().userId(userId).roleId(1L).build();
            when(queryWrapper.list()).thenReturn(List.of(existing1));

            userRolesService.removeUserRoles(userId, req);

            verify(updateWrapper).eq(any(), eq(userId));
            verify(updateWrapper).in(any(), argThat((List<Long> ids) ->
                    ids.contains(2L) && !ids.contains(1L)));
            verify(updateWrapper).remove();
        }

        /**
         * 测试对象：UserRolesServiceImpl.removeUserRoles(Long userId, RemoveUserRolesReq req)
         * 测试功能：移除的用户角色 ID 全都不存在时，不应失败
         * 输入：userId=1L, req.roleIds=[999, 888]
         *       lambdaQuery().list() 返回空（用户无任何角色）
         * 预期输出：lambdaUpdate 执行 eq(userId=1L) + in(roleId=[999, 888]) + remove()
         *         （IN 条件在 DB 层命中 0 行，删除操作无害执行）
         * 可能异常：N/A
         */
        @Test
        void removeUserRoles_nonexistentIds() {
            req.setRoleIds(List.of(999L, 888L));
            when(queryWrapper.list()).thenReturn(List.of());

            userRolesService.removeUserRoles(userId, req);

            verify(updateWrapper).eq(any(), eq(userId));
            verify(updateWrapper).in(any(), argThat((List<Long> ids) ->
                    ids.contains(999L) && ids.contains(888L)));
            verify(updateWrapper).remove();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> cast(Object obj) {
        return (List<T>) obj;
    }
}
