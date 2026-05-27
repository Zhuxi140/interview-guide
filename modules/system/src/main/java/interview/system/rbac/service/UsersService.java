package interview.system.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import interview.system.rbac.entity.SysUser;

/**
 * <p>
 * 系统用户主表（核心用户表，多张表依赖它） 服务类
 * </p>
 *
 * @author zhuxi
 * @since 2026-05-27
 */
public interface UsersService extends IService<SysUser> {

}
