package interview.system.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.system.rbac.mapper.PermissionsMapper;
import interview.system.rbac.model.entity.SysPermission;
import interview.system.rbac.service.PermissionService;
import org.springframework.stereotype.Service;

/**
 * @author zhuxi
 */

@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionsMapper, SysPermission> implements PermissionService {
}
