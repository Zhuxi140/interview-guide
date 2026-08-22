package interview.anticheat.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.anticheat.model.req.AntiCheatLogSearchReq;
import interview.anticheat.model.vo.AntiCheatEvidencePresignVO;
import interview.anticheat.model.vo.AntiCheatLogDetailVO;
import interview.anticheat.model.vo.AntiCheatLogListItemVO;
import interview.anticheat.service.AntiCheatService;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zhuxi
 * @apiNote 平台端防作弊检测日志查询（admin 路径控制器置于所属业务模块，参照 LocalMessageAdminController 先例）
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/admin/anti-cheat-logs")
@Tag(name = "防作弊检测日志（平台）")
@RequiredArgsConstructor
@Validated
public class AdminAntiCheatLogsController {

    private final AntiCheatService antiCheatService;

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.AntiCheat.ADMIN_LIST, scope = PermissionScope.PLATFORM)
    @Operation(summary = "分页查询防作弊检测日志（按事件类型/用户/会话筛选）")
    @GetMapping
    public Result<IPage<AntiCheatLogListItemVO>> pageLogs(
            @Valid @ParameterObject AntiCheatLogSearchReq req) {
        return Result.success(antiCheatService.pageLogs(req));
    }

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.AntiCheat.ADMIN_DETAIL, scope = PermissionScope.PLATFORM)
    @Operation(summary = "查询单条防作弊日志详情")
    @GetMapping("/{logId}")
    public Result<AntiCheatLogDetailVO> getLogDetail(@PathVariable Long logId) {
        return Result.success(antiCheatService.getLogDetail(logId));
    }

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(permissions = Perm.AntiCheat.ADMIN_DETAIL, scope = PermissionScope.PLATFORM)
    @Operation(summary = "获取防作弊证据短期下载地址")
    @GetMapping("/{logId}/evidence")
    public Result<AntiCheatEvidencePresignVO> getEvidencePresign(@PathVariable Long logId) {
        return Result.success(antiCheatService.getEvidencePresign(logId));
    }
}
