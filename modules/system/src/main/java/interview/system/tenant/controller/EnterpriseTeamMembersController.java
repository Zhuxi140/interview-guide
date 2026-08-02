package interview.system.tenant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequirePermission;
import interview.common.annonate.RequireActiveEnterprise;
import interview.system.tenant.model.req.TeamMemberCreateReq;
import interview.system.tenant.model.req.TeamMemberUpdateReq;
import interview.system.tenant.model.vo.TeamMemberCreateVO;
import interview.system.tenant.model.vo.TeamMemberItemVO;
import interview.system.tenant.service.EnterpriseTeamMembersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/**
 * @author zhuxi
 */
@RestController
@RequireActiveEnterprise
@RequestMapping(ApiVersion.V1 + "/enterprises/{enterpriseId}/members")
@Tag(name = "团队成员管理")
@RequiredArgsConstructor
@Validated
public class EnterpriseTeamMembersController {

    private final EnterpriseTeamMembersService teamMembersService;

    @MaxRiskLevel(RiskLevel.MID_RISK)
    @RequirePermission(permissions = Perm.Team.LIST, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "查询团队成员列表（分页）")
    @GetMapping
    public Result<IPage<TeamMemberItemVO>> listTeamMembers(
            @PathVariable("enterpriseId") Long enterpriseId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(defaultValue = "createdAt")
            @Pattern(regexp = "^createdAt$") String sort,
            @RequestParam(defaultValue = "desc")
            @Pattern(regexp = "^(?i)(asc|desc)$") String order
            ) {
        IPage<TeamMemberItemVO> pageResult = teamMembersService.listTeamMembers(enterpriseId, page, size,order);
        return Result.success(pageResult);
    }

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.Team.INVITE, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "邀请成员加入企业")
    @PostMapping
    public Result<TeamMemberCreateVO> inviteMember(@PathVariable("enterpriseId") Long enterpriseId,
                                                   @RequestBody @Valid TeamMemberCreateReq req) {
        TeamMemberCreateVO vo = teamMembersService.inviteMember(enterpriseId, req);
        return Result.success(vo);
    }

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.Team.UPDATE, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "修改成员角色")
    @PutMapping("/{memberId}")
    public Result<Void> updateMemberRole(@PathVariable("enterpriseId") Long enterpriseId,
                                                       @PathVariable("memberId") Long memberId,
                                                       @RequestBody @Valid TeamMemberUpdateReq req) {
        teamMembersService.updateMemberRole(enterpriseId, memberId, req);
        return Result.success();
    }

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.Team.REMOVE, scope = PermissionScope.ENTERPRISE)
    @Operation(summary = "移除团队成员")
    @DeleteMapping("/{memberId}")
    public Result<Void> removeMember(@PathVariable("enterpriseId") Long enterpriseId,
                                       @PathVariable("memberId") Long memberId) {
        teamMembersService.removeMember(enterpriseId, memberId);
        return Result.success();
    }
}
