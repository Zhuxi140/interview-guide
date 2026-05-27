package interview.system.rbac.service.impl;

import interview.system.rbac.entity.SysRolePermission;
import interview.system.rbac.service.RolePermissionsService;
import interview.system.rbac.mapper.RolePermissionsMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 角色权限关联表 服务实现类
 * </p>
 *
 * @author zhuxi
 * @since 2026-05-27
 */
@Service
public class RolePermissionsServiceImpl extends ServiceImpl<RolePermissionsMapper, SysRolePermission> implements RolePermissionsService {

}
