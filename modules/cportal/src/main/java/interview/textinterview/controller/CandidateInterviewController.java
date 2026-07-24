package interview.textinterview.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.constant.ApiVersion;
import interview.common.constant.Result;
import interview.textinterview.model.req.InterviewDecisionReq;
import interview.textinterview.model.req.InterviewScheduleCancelReq;
import interview.textinterview.model.vo.*;
import interview.textinterview.service.InterviewReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiVersion.V1 + "/candidate")
@Tag(name = "候选人面试排期与报告（C端）")
@RequiredArgsConstructor
@Validated
public class CandidateInterviewController {

    private final InterviewReportService interviewReportService;

    @Operation(summary = "C端查询我的面试排期（分页）")
    @GetMapping("/interview-schedules")
    public Result<IPage<InterviewScheduleCandidateListItemVO>> listMySchedules(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "interviewTime") String sort,
            @RequestParam(defaultValue = "asc") String order) {
        // TODO C端排期列表需调用schedule服务或跨模块查询
        return Result.success(null);
    }

    @Operation(summary = "C端接受或拒绝面试邀请")
    @PostMapping("/interview-schedules/{scheduleId}/decision")
    public Result<InterviewDecisionVO> decideSchedule(
            @PathVariable Long scheduleId,
            @Valid @RequestBody InterviewDecisionReq req) {
        // TODO 实现C端面试决策
        return Result.success(null);
    }

    @Operation(summary = "C端取消已确认但尚未开始的面试")
    @PostMapping("/interview-schedules/{scheduleId}/cancel")
    public Result<InterviewScheduleUpdateVO> cancelSchedule(
            @PathVariable Long scheduleId,
            @Valid @RequestBody InterviewScheduleCancelReq req) {
        // TODO 实现C端取消排期
        return Result.success(null);
    }

    @Operation(summary = "C端查询我的面评报告列表（分页）")
    @GetMapping("/interview-reports")
    public Result<IPage<InterviewReportCandidateListItemVO>> listMyReports(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) Integer size,
            @RequestParam(required = false) String generationStatus) {
        // TODO userId从AuthContext获取
        Long userId = 0L;
        return Result.success(interviewReportService.pageCandidateReports(userId, page, size, generationStatus));
    }

    @Operation(summary = "C端查询本人排期报告")
    @GetMapping("/interview-schedules/{scheduleId}/report")
    public Result<InterviewReportVO> getMyReport(@PathVariable Long scheduleId) {
        // TODO userId从AuthContext获取
        Long userId = 0L;
        return Result.success(interviewReportService.getCandidateReport(userId, scheduleId));
    }

    @Operation(summary = "获取本人报告PDF短期下载地址")
    @GetMapping("/interview-schedules/{scheduleId}/report/download")
    public Result<InterviewReportDownloadVO> downloadReport(@PathVariable Long scheduleId) {
        // TODO userId从AuthContext获取
        Long userId = 0L;
        return Result.success(interviewReportService.getDownloadUrl(userId, scheduleId));
    }
}
