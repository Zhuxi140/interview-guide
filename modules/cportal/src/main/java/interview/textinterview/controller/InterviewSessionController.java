package interview.textinterview.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.annonate.MaxRiskLevel;
import interview.common.constant.ApiVersion;
import interview.common.constant.Result;
import interview.common.enums.RiskLevel;
import interview.textinterview.model.req.InterviewAnswerSubmitReq;
import interview.textinterview.model.req.InterviewSessionCreateReq;
import interview.textinterview.model.req.InterviewSessionEndReq;
import interview.textinterview.model.vo.*;
import interview.textinterview.service.InterviewSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiVersion.V1 + "/interview-sessions")
@Tag(name = "文本面试会话（C端）")
@RequiredArgsConstructor
@Validated
public class InterviewSessionController {

    private final InterviewSessionService interviewSessionService;

    @Operation(summary = "候选人幂等创建文本面试会话（含首题）")
    @PostMapping
    public Result<InterviewSessionCreateVO> createSession(
            @Valid @RequestBody InterviewSessionCreateReq req) {
        return Result.success(interviewSessionService.createSession(req));
    }

    @Operation(summary = "查询面试会话状态")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @GetMapping("/{sessionId}")
    public Result<InterviewSessionVO> getSession(@PathVariable Long sessionId) {
        return Result.success(interviewSessionService.getSession(sessionId));
    }

    @Operation(summary = "候选人幂等提交当前题答案（AI实时评分+出下一题）")
    @PostMapping("/{sessionId}/answers")
    public Result<InterviewAnswerSubmitVO> submitAnswer(
            @PathVariable Long sessionId,
            @Valid @RequestBody InterviewAnswerSubmitReq req) {
        return Result.success(interviewSessionService.submitAnswer(sessionId, req));
    }

    @Operation(summary = "游标查询面试问答记录（含追问链路）")
    @GetMapping("/{sessionId}/history")
    public Result<IPage<InterviewHistoryItemVO>> getHistory(
            @PathVariable Long sessionId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "50") @Min(1) Integer size) {
        return Result.success(interviewSessionService.getHistory(sessionId, cursor, size));
    }

    @Operation(summary = "幂等结束面试会话（异步触发生成报告）")
    @PostMapping("/{sessionId}/end")
    public Result<InterviewSessionEndVO> endSession(
            @PathVariable Long sessionId,
            @Valid @RequestBody InterviewSessionEndReq req) {
        return Result.success(interviewSessionService.endSession(sessionId, req));
    }
}
