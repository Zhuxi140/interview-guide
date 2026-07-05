package interview.system.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.system.rbac.model.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 系统 API 资源表 Mapper 接口
 * </p>
 *
 * @author zhuxi
 */

@Mapper
public interface PermissionsMapper extends BaseMapper<SysPermission> {

    List<String> getPermCodeByRoleId(@Param("roleIds") List<Integer> roleIds);

}
