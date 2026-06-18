package interview.system.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.system.rbac.model.entity.SysRole;
import interview.system.rbac.model.vo.RoleDetailVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 系统角色表 Mapper 接口
 * </p>
 *
 * @author zhuxi
 */

@Mapper
public interface RolesMapper extends BaseMapper<SysRole> {

    RoleDetailVO selectAllPerByRoleId(@Param("roleId") Long roleId);

}
