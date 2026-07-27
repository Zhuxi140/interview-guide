package interview.voiceinterview.controller;

import interview.common.annonate.MaxRiskLevel;
import interview.common.constant.ApiVersion;
import interview.common.constant.Result;
import interview.common.enums.RiskLevel;
import interview.voiceinterview.model.req.EndSessionReq;
import interview.voiceinterview.model.vo.EndSessionVO;
import interview.voiceinterview.model.vo.EvaluationVO;
import interview.voiceinterview.model.vo.SessionDetailVO;
import interview.voiceinterview.model.vo.VoiceMessagePageVO;
import interview.voiceinterview.service.VoiceInterviewEvaluationService;
import interview.voiceinterview.service.VoiceInterviewMessageService;
import interview.voiceinterview.service.VoiceInterviewSessionService;
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

@RestController
@RequestMapping(ApiVersion.V1 + "/interview-sessions")
@Tag(name = "语音面试会话（C端）")
@RequiredArgsConstructor
@Validated
public class VoiceSessionController {

    private final VoiceInterviewSessionService voiceInterviewSessionService;
    private final VoiceInterviewMessageService voiceInterviewMessageService;
    private final VoiceInterviewEvaluationService voiceInterviewEvaluationService;

    @Operation(summary = "查询会话详情（含语音扩展信息）")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @GetMapping("/{sessionId}")
    public Result<SessionDetailVO> getSession(@PathVariable Long sessionId) {
        return Result.success(voiceInterviewSessionService.getSessionWithVoice(sessionId));
    }

    @Operation(summary = "幂等结束语音会话（异步触发评估）")
    @PostMapping("/{sessionId}/end")
    public Result<EndSessionVO> endSession(
            @PathVariable Long sessionId,
            @Valid @RequestBody EndSessionReq req) {
        return Result.success(voiceInterviewSessionService.endSession(sessionId, req));
    }

    @Operation(summary = "游标查询语音消息明细")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @GetMapping("/{sessionId}/voice/messages")
    public Result<VoiceMessagePageVO> queryMessages(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "0") Long afterSequence,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) Integer size) {
        return Result.success(
                voiceInterviewMessageService.queryMessages(sessionId, afterSequence, size));
    }

    @Operation(summary = "查询语音评估结果")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @GetMapping("/{sessionId}/voice/evaluation")
    public Result<EvaluationVO> getEvaluation(@PathVariable Long sessionId) {
        return Result.success(voiceInterviewEvaluationService.getEvaluation(sessionId));
    }
}
