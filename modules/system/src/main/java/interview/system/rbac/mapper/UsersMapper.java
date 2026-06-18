package interview.system.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.system.auth.model.entity.SysUser;

/**
 * <p>
 * 系统用户主表（核心用户表，多张表依赖它） Mapper 接口
 * </p>
 *
 * @author zhuxi
 */
public interface UsersMapper extends BaseMapper<SysUser> {

}
