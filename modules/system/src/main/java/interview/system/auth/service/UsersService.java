package interview.system.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import interview.system.auth.model.entity.SysUser;

/**
 * <p>
 * 系统用户主表（核心用户表，多张表依赖它） 服务类
 * </p>
 *
 * @author zhuxi
 */
public interface UsersService extends IService<SysUser> {

}
