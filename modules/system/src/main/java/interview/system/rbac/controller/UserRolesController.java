package interview.system.rbac.controller;

import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequirePermission;
import interview.system.rbac.model.req.AssignUserRolesReq;
import interview.system.rbac.model.req.RemoveUserRolesReq;
import interview.system.rbac.model.vo.UserRoleItemVO;
import interview.system.rbac.service.UserRolesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author zhuxi
 * @apiNote 用户角色关联
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/admin/users")
@Tag(name = "用户角色关联")
@AllArgsConstructor
public class UserRolesController {

    private final UserRolesService userRolesService;

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.AdminUserRoles.ASSIGN, scope = PermissionScope.PLATFORM)
    @Operation(summary = "为用户分配平台角色")
    @PostMapping("/{userId}/roles")
    public Result<Void> assignUserRoles(@PathVariable Long userId, @RequestBody @Valid AssignUserRolesReq req) {
        userRolesService.assignUserRoles(userId, req);
        return Result.success();
    }

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.AdminUserRoles.LIST, scope = PermissionScope.PLATFORM)
    @Operation(summary = "查询用户的平台角色")
    @GetMapping("/{userId}/roles")
    public Result<List<UserRoleItemVO>> getUserRoles(@PathVariable Long userId) {
        List<UserRoleItemVO> userRoles = userRolesService.getUserRoles(userId);
        return Result.success(userRoles);
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(permissions = Perm.AdminUserRoles.REMOVE, scope = PermissionScope.PLATFORM)
    @Operation(summary = "移除用户的指定平台角色")
    @DeleteMapping("/{userId}/roles")
    public Result<Void> removeUserRoles(@PathVariable Long userId, @RequestBody @Valid RemoveUserRolesReq req) {
        userRolesService.removeUserRoles(userId, req);
        return Result.success();
    }
}
