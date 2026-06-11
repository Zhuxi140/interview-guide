package interview.system.auth.service.impl;

import interview.system.auth.model.entity.SysUser;
import interview.system.auth.service.UsersService;
import interview.system.rbac.mapper.UsersMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 系统用户主表（核心用户表，多张表依赖它） 服务实现类
 * </p>
 *
 * @author zhuxi
 * @since 2026-05-27
 */
@Service
public class UsersServiceImpl extends ServiceImpl<UsersMapper, SysUser> implements UsersService {

}
