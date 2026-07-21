package interview.system.rbac.controller;

import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.RiskLevel;
import interview.common.annonate.MaxRiskLevel;
import interview.system.rbac.model.vo.RoleDetailVO;
import interview.system.rbac.model.vo.RoleListItemVO;
import interview.system.rbac.service.RolesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author zhuxi
 * @apiNote 角色管理
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/admin/roles")
@Tag(name = "角色查阅")
@RequiredArgsConstructor
public class RolesController {

    private final RolesService rolesService;

    @RequirePermission(permissions = Perm.AdminRoles.LIST)
    @MaxRiskLevel(RiskLevel.HIGH_RISK)
    @Operation(summary = "角色列表")
    @GetMapping
    public Result<List<RoleListItemVO>> listRoles() {
        return Result.success(rolesService.listRoles());
    }

    @RequirePermission(permissions = Perm.AdminRoles.DETAIL)
    @MaxRiskLevel(RiskLevel.HIGH_RISK)
    @Operation(summary = "角色详情（含绑定的权限）")
    @GetMapping("/{roleId}")
    public Result<RoleDetailVO> getRoleDetail(@PathVariable Long roleId) {
        return Result.success(rolesService.getRoleDetail(roleId));
    }
}
