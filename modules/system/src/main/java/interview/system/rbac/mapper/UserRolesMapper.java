package interview.system.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.system.rbac.model.entity.SysUserRole;
import interview.system.rbac.model.vo.UserRoleItemVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 用户角色关联表 Mapper 接口
 * </p>
 *
 * @author zhuxi
 * @since 2026-05-27
 */

@Mapper
public interface UserRolesMapper extends BaseMapper<SysUserRole> {

    List<UserRoleItemVO> getUserRoles(Long userId);

}
