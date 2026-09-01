package interview.system.rbac.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.system.rbac.mapper.PermissionsMapper;
import interview.system.rbac.model.entity.Permission;
import org.springframework.stereotype.Service;

/**
 * @author zhuxi
 */

@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionsMapper, Permission> implements PermissionService {
}
