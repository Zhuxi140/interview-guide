package interview.system.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.system.rbac.mapper.RolePermissionsMapper;
import interview.system.rbac.model.entity.SysRolePermission;
import interview.system.rbac.service.RolePermissionService;
import org.springframework.stereotype.Service;

/**
 * @author zhuxi
 */

@Service
public class RolePermissionServiceImpl extends ServiceImpl<RolePermissionsMapper, SysRolePermission> implements RolePermissionService {

}
