package interview.tutor.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import interview.tutor.model.req.TutorSessionCreateReq;
import interview.tutor.model.vo.TutorMessagePageVO;
import interview.tutor.model.vo.TutorSessionCreateVO;
import interview.tutor.model.vo.TutorSessionListItemVO;
import interview.tutor.service.AiTutorSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端 AI 考点答疑会话。
 *
 * @author zhuxi
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/candidate/tutor/sessions")
@Tag(name = "AI 答疑（C端）")
@RequiredArgsConstructor
@Validated
public class TutorSessionsController {

    private final AiTutorSessionService aiTutorSessionService;

    @Operation(summary = "基于本人已完成报告创建答疑会话")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(
            permissions = Perm.Tutor.CREATE_SESSION,
            scope = PermissionScope.PLATFORM
    )
    @PostMapping
    public Result<TutorSessionCreateVO> createSession(
            @Valid @RequestBody TutorSessionCreateReq req) {
        return Result.success(aiTutorSessionService.createSession(req));
    }

    @Operation(summary = "查询本人答疑会话列表（分页）")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(
            permissions = Perm.Tutor.LIST_SESSIONS,
            scope = PermissionScope.PLATFORM
    )
    @GetMapping
    public Result<IPage<TutorSessionListItemVO>> listMySessions(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size) {
        return Result.success(aiTutorSessionService.pageMySessions(page, size));
    }

    @Operation(summary = "游标查询会话答疑消息")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(
            permissions = Perm.Tutor.LIST_MESSAGES,
            scope = PermissionScope.PLATFORM
    )
    @GetMapping("/{sessionId}/messages")
    public Result<TutorMessagePageVO> listMessages(
            @PathVariable Long sessionId,
            @RequestParam(required = false) @Min(0) Long cursor,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) Integer size) {
        return Result.success(aiTutorSessionService.listMessages(sessionId, cursor, size));
    }
}
