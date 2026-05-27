package interview.system.rbac.service.impl;

import interview.system.rbac.entity.SysRole;
import interview.system.rbac.service.RolesService;
import interview.system.rbac.mapper.RolesMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 系统角色表 服务实现类
 * </p>
 *
 * @author zhuxi
 * @since 2026-05-27
 */
@Service
public class RolesServiceImpl extends ServiceImpl<RolesMapper, SysRole> implements RolesService {

}
