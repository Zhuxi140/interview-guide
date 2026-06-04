package interview.system.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.system.rbac.model.entity.SysRole;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * <p>
 * 系统角色表 Mapper 接口
 * </p>
 *
 * @author zhuxi
 * @since 2026-05-27
 */

@Mapper
public interface RolesMapper extends BaseMapper<SysRole> {


    @Select(
    """
    SELECT sr.role_code roleCode FROM sys_roles sr JOIN sys_user_roles sur ON sr.id = sur.role_id WHERE sur.user_id = #{userId}
    """
    )
    List<String> selectRoleCodesByUserId(Long userId);

}
