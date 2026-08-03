package interview.textinterview.controller;

import interview.common.annonate.RequireActiveEnterprise;
import interview.common.constant.ApiVersion;
import interview.common.constant.Result;
import interview.textinterview.model.req.InterviewTakeoverEndReq;
import interview.textinterview.model.req.InterviewTakeoverStartReq;
import interview.textinterview.model.vo.InterviewTakeoverEndVO;
import interview.textinterview.model.vo.InterviewTakeoverStartVO;
import interview.textinterview.service.InterviewTakeoverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequireActiveEnterprise
@RequestMapping(ApiVersion.V1 + "/enterprises/{enterpriseId}/interview-sessions/{sessionId}/takeovers")
@Tag(name = "面试官人工接管（B端）")
@RequiredArgsConstructor
@Validated
public class InterviewTakeoverController {

    private final InterviewTakeoverService interviewTakeoverService;

    @Operation(summary = "面试官接管会话并暂停AI自动提问")
    @PostMapping
    public Result<InterviewTakeoverStartVO> startTakeover(
            @PathVariable Long enterpriseId,
            @PathVariable Long sessionId,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody InterviewTakeoverStartReq req) {
        return Result.success(interviewTakeoverService.startTakeover(
                enterpriseId, sessionId, idempotencyKey, req));
    }

    @Operation(summary = "结束人工接管")
    @PostMapping("/{takeoverId}/end")
    public Result<InterviewTakeoverEndVO> endTakeover(
            @PathVariable Long enterpriseId,
            @PathVariable Long sessionId,
            @PathVariable Long takeoverId,
            @Valid @RequestBody InterviewTakeoverEndReq req) {
        return Result.success(interviewTakeoverService.endTakeover(
                enterpriseId, sessionId, takeoverId, req));
    }
}
