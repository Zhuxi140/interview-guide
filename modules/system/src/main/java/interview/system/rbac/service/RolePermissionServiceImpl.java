package interview.system.rbac.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.system.rbac.mapper.RolePermissionsMapper;
import interview.system.rbac.model.entity.RolePermission;
import org.springframework.stereotype.Service;

/**
 * @author zhuxi
 */

@Service
public class RolePermissionServiceImpl extends ServiceImpl<RolePermissionsMapper, RolePermission> implements RolePermissionService {

}
