package interview.system.rbac.aspect;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import interview.common.enums.Logic;
import interview.common.enums.PermissionScope;
import interview.common.enums.Role;
import interview.common.exception.AccessDeniedException;
import interview.common.annonate.RequirePermission;
import interview.framework.context.AuthContext;
import interview.system.rbac.mapper.PermissionsMapper;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author zhuxi
 */

@Component
@Aspect
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionsMapper permissionsMapper;

    @Around("@annotation(requirePermission)")
    public Object preHandle(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {

        PermissionScope scope = requirePermission.scope();
        List<Integer> roleIds;

        // 从方法参数提取 enterpriseId，用于和 JWT 上下文当前活跃企业比对
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        Long pathEnterpriseId = null;
        for (int i = 0; i < paramNames.length; i++) {
            if ("enterpriseId".equals(paramNames[i]) && args[i] instanceof Long) {
                pathEnterpriseId = (Long) args[i];
                break;
            }
        }

        if (scope == PermissionScope.PLATFORM) {
            List<Role> platformRoles = AuthContext.getAuthContext().platformRoleCodes();

            // 超级管理员，直接放行
            if (!CollectionUtils.isEmpty(platformRoles) && platformRoles.contains(Role.SUPER_ADMIN)){
                return joinPoint.proceed();
            }
            roleIds = platformRoles.stream()
                    .map(Role::getCode)
                    .toList();
        } else if (scope == PermissionScope.ENTERPRISE) {
            if (pathEnterpriseId != null && !pathEnterpriseId.equals(AuthContext.getEnterpriseId())) {
                throw new AccessDeniedException();
            }
            Long enterpriseId = AuthContext.getEnterpriseId();
            List<Role> enterpriseRoles = AuthContext.getAuthContext().entRoleMap().get(enterpriseId);
            if (!CollectionUtils.isEmpty(enterpriseRoles) && enterpriseRoles.contains(Role.ENTERPRISE_OWNER)){
                return joinPoint.proceed();
            }
            roleIds = enterpriseRoles.stream().map(Role::getCode).toList();
        } else if (scope == PermissionScope.BOTH) {
            if (pathEnterpriseId != null && !pathEnterpriseId.equals(AuthContext.getEnterpriseId())) {
                throw new AccessDeniedException();
            }

            List<Role> platformRoles = AuthContext.getAuthContext().platformRoleCodes();
            if (!CollectionUtils.isEmpty(platformRoles) && platformRoles.contains(Role.SUPER_ADMIN)){
                return joinPoint.proceed();
            }

            Long enterpriseId = AuthContext.getEnterpriseId();
            List<Role> enterpriseRoles = enterpriseId != null
                    ? AuthContext.getAuthContext().entRoleMap().get(enterpriseId)
                    : null;
            if (!CollectionUtils.isEmpty(enterpriseRoles) && enterpriseRoles.contains(Role.ENTERPRISE_OWNER)){
                return joinPoint.proceed();
            }

            List<Integer> platformRoleIds = !CollectionUtils.isEmpty(platformRoles)
                    ? platformRoles.stream().map(Role::getCode).toList()
                    : List.of();
            List<Integer> enterpriseRoleIds = !CollectionUtils.isEmpty(enterpriseRoles)
                    ? enterpriseRoles.stream().map(Role::getCode).toList()
                    : List.of();

            roleIds = new ArrayList<>(platformRoleIds.size() + enterpriseRoleIds.size());
            roleIds.addAll(platformRoleIds);
            roleIds.addAll(enterpriseRoleIds);
        } else {
            roleIds = List.of();
        }

        // 如果没有提取到任何有效角色，直接抛出权限不足
        if (roleIds.isEmpty()){
            throw new AccessDeniedException();
        }

        List<String> permCodeByRoleId = permissionsMapper.getPermCodeByRoleId(roleIds);

        Logic logic = requirePermission.logic();
        Set<String> required = Set.of(requirePermission.permissions());
        Set<String> userPerms = Set.copyOf(permCodeByRoleId);

        boolean hasPermission = false;
        if (Logic.AND.equals(logic)){
            hasPermission = userPerms.containsAll(required);
        }else{
            hasPermission = required.stream().anyMatch(userPerms::contains);
        }

        if (!hasPermission) {
            throw new AccessDeniedException();
        }

        return joinPoint.proceed();
    }
}
