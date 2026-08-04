package interview.voiceinterview.controller;

import interview.common.annonate.MaxRiskLevel;
import interview.common.constant.ApiVersion;
import interview.common.constant.Result;
import interview.common.enums.RiskLevel;
import interview.voiceinterview.model.vo.EvaluationVO;
import interview.voiceinterview.model.vo.VoiceMessagePageVO;
import interview.voiceinterview.service.VoiceInterviewEvaluationService;
import interview.voiceinterview.service.VoiceInterviewMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiVersion.V1 + "/interview-sessions")
@Tag(name = "语音面试会话（C端）")
@RequiredArgsConstructor
@Validated
public class VoiceSessionController {

    private final VoiceInterviewMessageService voiceInterviewMessageService;
    private final VoiceInterviewEvaluationService voiceInterviewEvaluationService;

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
