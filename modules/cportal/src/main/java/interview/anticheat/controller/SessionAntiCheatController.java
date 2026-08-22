package interview.anticheat.controller;

import interview.anticheat.model.req.AntiCheatEvidenceUploadTicketReq;
import interview.anticheat.model.vo.AntiCheatEvidenceUploadTicketVO;
import interview.anticheat.service.AntiCheatService;
import interview.common.annonate.MaxRiskLevel;
import interview.common.constant.ApiVersion;
import interview.common.constant.Result;
import interview.common.enums.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zhuxi
 * @apiNote C 端防作弊证据上传（候选人自查会话，不加权限注解）
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/interview-sessions/{sessionId}/anti-cheat-evidence")
@Tag(name = "防作弊证据上传（C端）")
@RequiredArgsConstructor
@Validated
public class SessionAntiCheatController {

    private final AntiCheatService antiCheatService;

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @Operation(summary = "获取防作弊证据短期上传凭证（会话参与者校验）")
    @PostMapping("/upload-ticket")
    public Result<AntiCheatEvidenceUploadTicketVO> createEvidenceUploadTicket(
            @PathVariable Long sessionId,
            @RequestBody @Valid AntiCheatEvidenceUploadTicketReq req) {
        return Result.success(antiCheatService.createEvidenceUploadTicket(sessionId, req));
    }
}
