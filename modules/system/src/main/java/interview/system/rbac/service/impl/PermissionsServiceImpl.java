package interview.system.rbac.service.impl;

import interview.system.rbac.model.entity.SysPermission;
import interview.system.rbac.service.PermissionsService;
import interview.system.rbac.mapper.PermissionsMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 系统 API 资源表 服务实现类
 * </p>
 *
 * @author zhuxi
 * @since 2026-05-27
 */
@Service
public class PermissionsServiceImpl extends ServiceImpl<PermissionsMapper, SysPermission> implements PermissionsService {

}
