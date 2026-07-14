package interview.system.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.system.rbac.mapper.RolesMapper;
import interview.system.rbac.model.entity.Role;
import interview.system.rbac.model.vo.RoleDetailVO;
import interview.system.rbac.model.vo.RoleListItemVO;
import interview.system.rbac.service.RolesService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author zhuxi
 * @apiNote 角色服务实现类
 */


@Service
@AllArgsConstructor
public class RolesServiceImpl extends ServiceImpl<RolesMapper, Role> implements RolesService {

    private final RolesMapper rolesMapper;

    @Override
    public List<RoleListItemVO> listRoles() {
        List<Role> roleList = lambdaQuery()
                .select(
                        Role::getId,
                        Role::getRoleCode,
                        Role::getRoleName,
                        Role::getRoleScope,
                        Role::getCreatedAt
                        )
                .list();

        return roleList.stream()
                .map(role -> RoleListItemVO.builder()
                        .id(role.getId())
                        .roleCode(role.getRoleCode())
                        .roleName(role.getRoleName())
                        .roleScope(role.getRoleScope().name())
                        .createdAt(role.getCreatedAt())
                        .build())
                .toList();
    }

    @Override
    public RoleDetailVO getRoleDetail(Long roleId) {
        return rolesMapper.selectAllPerByRoleId(roleId);
    }
}
