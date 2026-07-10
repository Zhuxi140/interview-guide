package interview.system.rbac;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import interview.common.enums.RoleScope;
import interview.system.rbac.mapper.RolesMapper;
import interview.system.rbac.model.entity.SysRole;
import interview.system.rbac.model.vo.RoleDetailVO;
import interview.system.rbac.model.vo.RoleListItemVO;
import interview.system.rbac.service.impl.RolesServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static interview.system.TestMockUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolesServiceImplTest {

    @Mock
    private RolesMapper rolesMapper;

    private RolesServiceImpl rolesService;
    private LambdaQueryChainWrapper<SysRole> roleQueryWrapper;

    @BeforeEach
    void setUp() {
        rolesService = spy(new RolesServiceImpl(rolesMapper));
        roleQueryWrapper = mockQueryWrapper();
    }

    @Nested
    class ListRoles {

        /**
         * 测试对象：RolesServiceImpl.listRoles()
         * 测试功能：查询全部角色列表，验证 MyBatis-Plus lambdaQuery 调用链路
         *          以及 SysRole → RoleListItemVO 的字段映射
         * 输入：roleQueryWrapper.list() 返回 2 条 SysRole
         *       (PLATFORM_ADMIN / PLATFORM, HR_MANAGER / ENTERPRISE)
         * 预期输出：返回 List<RoleListItemVO> size=2
         *         第 0 条 id=1, roleCode=PLATFORM_ADMIN, roleScope=PLATFORM
         *         第 1 条 roleCode=HR_MANAGER, roleScope=ENTERPRISE
         * 可能异常：N/A
         */
        @Test
        void listRoles_success() {
            SysRole role1 = new SysRole();
            role1.setId(1);
            role1.setRoleCode("PLATFORM_ADMIN");
            role1.setRoleName("平台管理员");
            role1.setRoleScope(RoleScope.PLATFORM);
            role1.setCreatedAt(OffsetDateTime.now());

            SysRole role2 = new SysRole();
            role2.setId(2);
            role2.setRoleCode("HR_MANAGER");
            role2.setRoleName("HR管理员");
            role2.setRoleScope(RoleScope.ENTERPRISE);
            role2.setCreatedAt(OffsetDateTime.now());

            when(roleQueryWrapper.list()).thenReturn(List.of(role1, role2));
            doReturn(roleQueryWrapper).when(rolesService).lambdaQuery();

            List<RoleListItemVO> result = rolesService.listRoles();

            assertEquals(2, result.size());
            assertEquals(1, result.get(0).id());
            assertEquals("PLATFORM_ADMIN", result.get(0).roleCode());
            assertEquals("PLATFORM", result.get(0).roleScope());
            assertEquals("HR_MANAGER", result.get(1).roleCode());
            assertEquals("ENTERPRISE", result.get(1).roleScope());
        }

        /**
         * 测试对象：RolesServiceImpl.listRoles()
         * 测试功能：角色表为空时，应返回空列表而非 null 或抛异常
         * 输入：roleQueryWrapper.list() 返回空 List
         * 预期输出：返回空 List<RoleListItemVO>，isEmpty() == true
         * 可能异常：N/A
         */
        @Test
        void listRoles_empty() {
            when(roleQueryWrapper.list()).thenReturn(List.of());
            doReturn(roleQueryWrapper).when(rolesService).lambdaQuery();

            List<RoleListItemVO> result = rolesService.listRoles();

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class GetRoleDetail {

        private final Long roleId = 1L;

        /**
         * 测试对象：RolesServiceImpl.getRoleDetail(Long roleId)
         * 测试功能：查询角色详情（含权限列表），验证 rolesMapper XML 查询结果
         *          原样返回至 Controller 层
         * 输入：roleId=1L
         *       rolesMapper.selectAllPerByRoleId(1L) 返回 RoleDetailVO
         *       (roleCode=PLATFORM_ADMIN, permissions=[user:create, user:read, user:delete])
         * 预期输出：返回 RoleDetailVO 非空，roleCode=PLATFORM_ADMIN,
         *         permissions size=3, 包含 user:read
         * 可能异常：N/A（Mapper 层异常由 MyBatis 处理，此处仅测 Service）
         */
        @Test
        void getRoleDetail_success() {
            RoleDetailVO expected = RoleDetailVO.builder()
                    .id(roleId).roleCode("PLATFORM_ADMIN").roleName("平台管理员")
                    .roleScope("PLATFORM")
                    .permissions(List.of("user:create", "user:read", "user:delete"))
                    .createdAt(OffsetDateTime.now())
                    .build();

            when(rolesMapper.selectAllPerByRoleId(roleId)).thenReturn(expected);

            RoleDetailVO result = rolesService.getRoleDetail(roleId);

            assertNotNull(result);
            assertEquals("PLATFORM_ADMIN", result.roleCode());
            assertEquals(3, result.permissions().size());
            assertTrue(result.permissions().contains("user:read"));
        }

        /**
         * 测试对象：RolesServiceImpl.getRoleDetail(Long roleId)
         * 测试功能：查询不存在的角色 ID，验证返回 null 的处理
         * 输入：roleId=999L（数据库中不存在）
         *       rolesMapper.selectAllPerByRoleId(999L) 返回 null
         * 预期输出：返回 null（Controller 层应返回 200 + data=null 或 404）
         * 可能异常：N/A（业务层未做非空校验，直接透传 null）
         */
        @Test
        void getRoleDetail_notFound() {
            when(rolesMapper.selectAllPerByRoleId(999L)).thenReturn(null);

            RoleDetailVO result = rolesService.getRoleDetail(999L);

            assertNull(result);
        }

        /**
         * 测试对象：RolesServiceImpl.getRoleDetail(Long roleId)
         * 测试功能：角色存在但未绑定任何权限时，permissions 应为空列表而非 null
         * 输入：roleId=1L
         *       rolesMapper.selectAllPerByRoleId(1L) 返回 RoleDetailVO
         *       (permissions=空 List)
         * 预期输出：返回 RoleDetailVO 非空，permissions.isEmpty() == true
         * 可能异常：N/A
         */
        @Test
        void getRoleDetail_noPermissions() {
            RoleDetailVO expected = RoleDetailVO.builder()
                    .id(roleId).roleCode("VIEWER").roleName("观察者")
                    .roleScope("PLATFORM")
                    .permissions(List.of())
                    .createdAt(OffsetDateTime.now())
                    .build();

            when(rolesMapper.selectAllPerByRoleId(roleId)).thenReturn(expected);

            RoleDetailVO result = rolesService.getRoleDetail(roleId);

            assertNotNull(result);
            assertTrue(result.permissions().isEmpty());
        }
    }
}
