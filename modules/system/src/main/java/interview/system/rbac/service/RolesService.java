package interview.system.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import interview.system.rbac.model.entity.Role;
import interview.system.rbac.model.vo.RoleDetailVO;
import interview.system.rbac.model.vo.RoleListItemVO;

import java.util.List;

/**
 * @author zhuxi
 * @apiNote 角色服务
 */
public interface RolesService extends IService<Role> {

    /**
     * 获取角色列表
     * @return 角色列表
     */
    List<RoleListItemVO> listRoles();

    /**
     * 获取角色详情
     * @param roleId 角色ID
     * @return 角色详情
     */
    RoleDetailVO getRoleDetail(Long roleId);
}
