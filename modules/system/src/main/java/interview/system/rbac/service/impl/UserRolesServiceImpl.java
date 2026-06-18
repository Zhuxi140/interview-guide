package interview.system.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.system.auth.model.entity.SysUser;
import interview.system.auth.service.impl.UsersServiceImpl;
import interview.system.rbac.mapper.UserRolesMapper;
import interview.system.rbac.model.entity.SysRole;
import interview.system.rbac.model.entity.SysUserRole;
import interview.system.rbac.model.req.AssignUserRolesReq;
import interview.system.rbac.model.req.RemoveUserRolesReq;
import interview.system.rbac.model.vo.UserRoleItemVO;
import interview.system.rbac.service.UserRolesService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * @author zhuxi
 * @apiNote 用户角色服务
 */
@Service
@AllArgsConstructor
public class UserRolesServiceImpl extends ServiceImpl<UserRolesMapper, SysUserRole> implements UserRolesService {

    private final UserRolesMapper userRolesMapper;
    private final RolesServiceImpl rolesService;
    private final UsersServiceImpl usersService;

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void assignUserRoles(Long userId, AssignUserRolesReq req) {

        List<Long> roleIds = req.getRoleIds().stream().distinct().toList();

        //FK 检查
        FKCheck(userId, roleIds);

        List<Long> existing = lambdaQuery()
                .eq(SysUserRole::getUserId, userId)
                .select(SysUserRole::getRoleId)
                .list()
                .stream()
                .map(SysUserRole::getRoleId)
                .toList();

        List<SysUserRole> toInsert = roleIds.stream()
                .filter(rid -> !existing.contains(rid))
                .map(roleId -> SysUserRole.builder()
                        .userId(userId)
                        .roleId(roleId)
                        .build())
                .toList();

        if (!toInsert.isEmpty()) {
            saveBatch(toInsert);
        }
    }

    @Override
    public List<UserRoleItemVO> getUserRoles(Long userId) {
        return userRolesMapper.getUserRoles(userId);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void removeUserRoles(Long userId, RemoveUserRolesReq req) {
        List<Long> roleIds = req.getRoleIds().stream().distinct().toList();

        List<Long> existing = lambdaQuery()
                .eq(SysUserRole::getUserId, userId)
                .select(SysUserRole::getRoleId)
                .list()
                .stream()
                .map(SysUserRole::getRoleId)
                .toList();

        lambdaUpdate()
                .eq(SysUserRole::getUserId, userId)
                .in(SysUserRole::getRoleId,
                        roleIds.stream()
                        .filter(existing::contains)
                        .toList())
                .remove();
    }


    public void FKCheck(Long userId, List<Long> roleId) {
        if (!usersService.lambdaQuery()
                        .eq(SysUser::getId, userId)
                        .exists()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DATA_ANOMALY);
        }
        if (!rolesService.lambdaQuery()
                    .in(SysRole::getId, roleId)
                    .exists()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DATA_ANOMALY);
        }
    }
}
