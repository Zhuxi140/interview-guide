package interview.textinterview.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.annonate.MaxRiskLevel;
import interview.common.constant.ApiVersion;
import interview.common.constant.Result;
import interview.common.enums.RiskLevel;
import interview.textinterview.model.req.InterviewSessionEndReq;
import interview.textinterview.model.vo.*;
import interview.textinterview.service.InterviewSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
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

    @Operation(summary = "查询面试会话状态")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @GetMapping("/{sessionId}")
    public Result<InterviewSessionVO> getSession(@PathVariable Long sessionId) {
        return Result.success(interviewSessionService.getSession(sessionId));
    }

    @Operation(summary = "幂等结束面试会话（异步触发生成报告）")
    @PostMapping("/{sessionId}/end")
    public Result<InterviewSessionEndVO> endSession(
            @PathVariable Long sessionId,
            @Valid @RequestBody InterviewSessionEndReq req) {
        return Result.success(interviewSessionService.endSession(sessionId, req));
    }

    @Operation(summary = "按序号增量查询可回放语义事件")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @GetMapping("/{sessionId}/timeline")
    public Result<InterviewTimelinePageVO> getTimeline(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "0") Long afterSequence,
            @RequestParam(defaultValue = "100") @Min(1) @Max(100) Integer size) {
        return Result.success(interviewSessionService.getTimeline(sessionId, afterSequence, size));
    }

    @Operation(summary = "查询本人已提交作答（REST 兜底：断线后恢复现场）")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @GetMapping("/{sessionId}/answers")
    public Result<IPage<InterviewAnswerListItemVO>> getAnswers(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size) {
        return Result.success(interviewSessionService.pageAnswers(sessionId, page, size));
    }
}
