package interview.system.rbac.service.impl;

import interview.system.rbac.model.entity.SysUserRole;
import interview.system.rbac.service.UserRolesService;
import interview.system.rbac.mapper.UserRolesMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户角色关联表 服务实现类
 * </p>
 *
 * @author zhuxi
 * @since 2026-05-27
 */
@Service
public class UserRolesServiceImpl extends ServiceImpl<UserRolesMapper, SysUserRole> implements UserRolesService {

}
