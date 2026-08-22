package interview.system.rbac.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.ErrorCode;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequirePermission;
import interview.common.exception.BusinessException;
import interview.system.auth.model.entity.User;
import interview.system.auth.service.UsersService;
import interview.system.rbac.model.req.AdminUserSearchReq;
import interview.system.rbac.model.vo.AdminUserDetailVO;
import interview.system.rbac.model.vo.AdminUserListItemVO;
import interview.system.rbac.service.UserRolesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @author zhuxi
 * @apiNote 平台用户管理（禁用/解封与重置密码见后续迭代）
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/admin/users")
@Tag(name = "平台用户管理")
@AllArgsConstructor
public class UsersController {

    private final UsersService usersService;
    private final UserRolesService userRolesService;

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.AdminUsers.LIST, scope = PermissionScope.PLATFORM)
    @Operation(summary = "平台用户分页查询")
    @GetMapping
    public Result<IPage<AdminUserListItemVO>> pageUsers(@Valid AdminUserSearchReq req) {
        return Result.success(usersService.pageUsers(req));
    }

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.AdminUsers.DETAIL, scope = PermissionScope.PLATFORM)
    @Operation(summary = "平台用户详情（含平台角色）")
    @GetMapping("/{userId}")
    public Result<AdminUserDetailVO> getUserDetail(@PathVariable Long userId) {
        User user = usersService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return Result.success(AdminUserDetailVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .userType(user.getUserType() == null ? null : user.getUserType().name())
                .status(user.getStatus() == null ? null : user.getStatus().name())
                .riskLevel(user.getRiskLevel() == null ? null : user.getRiskLevel().name())
                .platformRoles(userRolesService.getUserRoles(userId))
                .createdAt(user.getCreatedAt())
                .build());
    }
}
