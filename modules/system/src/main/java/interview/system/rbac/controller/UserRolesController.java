package interview.system.rbac.controller;

import interview.common.constant.ApiVersion;
import interview.common.constant.Result;
import interview.system.rbac.model.req.AssignUserRolesReq;
import interview.system.rbac.model.req.RemoveUserRolesReq;
import interview.system.rbac.model.vo.UserRoleItemVO;
import interview.system.rbac.service.UserRolesService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    @ApiResponse(description = "为用户分配平台角色")
    @PostMapping("/{userId}/roles")
    public Result<Void> assignUserRoles(@PathVariable Long userId, @RequestBody @Valid AssignUserRolesReq req) {
        userRolesService.assignUserRoles(userId, req);
        return Result.success();
    }

    @ApiResponse(description = "查询用户的平台角色")
    @GetMapping("/{userId}/roles")
    public Result<List<UserRoleItemVO>> getUserRoles(@PathVariable Long userId) {
        List<UserRoleItemVO> userRoles = userRolesService.getUserRoles(userId);
        return Result.success(userRoles);
    }

    @ApiResponse(description = "移除用户的指定平台角色")
    @DeleteMapping("/{userId}/roles")
    public Result<Void> removeUserRoles(@PathVariable Long userId, @RequestBody @Valid RemoveUserRolesReq req) {
        userRolesService.removeUserRoles(userId, req);
        return Result.success();
    }
}
