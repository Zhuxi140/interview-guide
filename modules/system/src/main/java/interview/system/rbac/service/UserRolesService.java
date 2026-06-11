package interview.system.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import interview.system.rbac.model.entity.SysUserRole;
import interview.system.rbac.model.req.AssignUserRolesReq;
import interview.system.rbac.model.req.RemoveUserRolesReq;
import interview.system.rbac.model.vo.UserRoleItemVO;

import java.util.List;

/**
 * @author zhuxi
 * @apiNote 用户角色服务
 * @since 2026/5/27 14:09
 */

public interface UserRolesService extends IService<SysUserRole> {

    /**
     * 分配用户角色
     * @param userId 用户ID
     * @param req 角色ID列表
     */
    void assignUserRoles(Long userId, AssignUserRolesReq req);

    /**
     * 获取用户角色列表
     * @param userId 用户ID
     * @return 角色列表
     */
    List<UserRoleItemVO> getUserRoles(Long userId);

    /**
     * 移除用户角色
     * @param userId 用户ID
     * @param req 角色ID列表
     */
    void removeUserRoles(Long userId, RemoveUserRolesReq req);
}
