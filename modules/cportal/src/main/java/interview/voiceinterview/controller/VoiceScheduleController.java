package interview.voiceinterview.controller;

import interview.common.constant.ApiVersion;
import interview.common.constant.Result;
import interview.voiceinterview.model.vo.JoinTokenVO;
import interview.voiceinterview.service.VoiceInterviewSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiVersion.V1 + "/interview-schedules")
@Tag(name = "语音面试排期（C端）")
@RequiredArgsConstructor
@Validated
public class VoiceScheduleController {

    private final VoiceInterviewSessionService voiceInterviewSessionService;

    @Operation(summary = "生成语音面试连接凭证（首次创建/断线重连）")
    @PostMapping("/{scheduleId}/join-token")
    public Result<JoinTokenVO> generateJoinToken(@PathVariable Long scheduleId) {
        return Result.success(voiceInterviewSessionService.generateJoinToken(scheduleId));
    }
}
