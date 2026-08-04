package interview.textinterview.controller;

import interview.common.constant.ApiVersion;
import interview.common.constant.Result;
import interview.textinterview.model.vo.InterviewJoinTokenVO;
import interview.textinterview.service.InterviewSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiVersion.V1 + "/interview-schedules")
@Tag(name = "统一面试会话入口")
@RequiredArgsConstructor
@Validated
public class InterviewScheduleSessionController {

    private final InterviewSessionService interviewSessionService;

    @Operation(summary = "创建或复用面试会话并签发连接凭证")
    @PostMapping("/{scheduleId}/join-token")
    public Result<InterviewJoinTokenVO> generateJoinToken(
            @PathVariable Long scheduleId,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey) {
        return Result.success(interviewSessionService.generateJoinToken(scheduleId, idempotencyKey));
    }
}
