package interview.system.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.system.auth.service.UsersService;
import interview.system.rbac.service.RolesService;
import interview.common.enums.RoleScope;
import org.springframework.dao.DataIntegrityViolationException;
import interview.system.auth.model.entity.User;
import interview.system.rbac.mapper.UserRolesMapper;
import interview.system.rbac.model.entity.Role;
import interview.system.rbac.model.entity.UserRole;
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
public class UserRolesServiceImpl extends ServiceImpl<UserRolesMapper, UserRole> implements UserRolesService {

    private final UserRolesMapper userRolesMapper;
    private final RolesService rolesService;
    private final UsersService usersService;

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void assignUserRoles(Long userId, AssignUserRolesReq req) {

        List<Integer> roleIds = req.getRoleIds().stream().distinct().toList();

        //FK 检查
        FKCheck(userId, roleIds);

        List<Integer> existing = lambdaQuery()
                .eq(UserRole::getUserId, userId)
                .select(UserRole::getRoleId)
                .list()
                .stream()
                .map(UserRole::getRoleId)
                .toList();

        List<UserRole> toInsert = roleIds.stream()
                .filter(rid -> !existing.contains(rid))
                .map(roleId -> UserRole.builder()
                        .userId(userId)
                        .roleId(roleId)
                        .build())
                .toList();

        if (toInsert.isEmpty()) {
            throw new BusinessException(ErrorCode.ROLE_ALREADY_ASSIGNED);
        }

        try {
            saveBatch(toInsert);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.ROLE_ALREADY_ASSIGNED);
        }
    }

    @Override
    public List<UserRoleItemVO> getUserRoles(Long userId) {
        return userRolesMapper.getUserRoles(userId);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void removeUserRoles(Long userId, RemoveUserRolesReq req) {
        List<Integer> roleIds = req.getRoleIds().stream().distinct().toList();

        List<Integer> existing = lambdaQuery()
                .eq(UserRole::getUserId, userId)
                .select(UserRole::getRoleId)
                .list()
                .stream()
                .map(UserRole::getRoleId)
                .toList();

        lambdaUpdate()
                .eq(UserRole::getUserId, userId)
                .in(UserRole::getRoleId,
                        roleIds.stream()
                        .filter(existing::contains)
                        .toList())
                .remove();
    }


    public void FKCheck(Long userId, List<Integer> roleId) {
        if (!usersService.lambdaQuery()
                        .eq(User::getId, userId)
                        .exists()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DATA_ANOMALY);
        }
        long validCount = rolesService.lambdaQuery()
                    .in(Role::getId, roleId)
                    .eq(Role::getRoleScope, RoleScope.PLATFORM)
                    .count();
        if (validCount != roleId.size()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DATA_ANOMALY);
        }
    }
}
