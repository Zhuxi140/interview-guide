package interview.system.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.system.rbac.model.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 系统 API 资源表 Mapper 接口
 * </p>
 *
 * @author zhuxi
 */

@Mapper
public interface PermissionsMapper extends BaseMapper<Permission> {

    List<String> getPermCodeByRoleId(@Param("roleIds") List<Integer> roleIds);

}
