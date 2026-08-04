package interview.textinterview.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.annonate.MaxRiskLevel;
import interview.common.constant.ApiVersion;
import interview.common.constant.Result;
import interview.common.enums.InterviewReportGenerationStatus;
import interview.common.enums.InterviewScheduleStatus;
import interview.common.enums.RiskLevel;
import interview.framework.context.AuthContext;
import interview.textinterview.model.req.CandidateInterviewAvailabilityUpdateReq;
import interview.textinterview.model.req.InterviewDecisionReq;
import interview.textinterview.model.vo.*;
import interview.textinterview.service.CandidateInterviewAvailabilityService;
import interview.textinterview.service.CandidateInterviewDecisionService;
import interview.textinterview.service.CandidateInterviewQueryService;
import interview.textinterview.service.InterviewReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
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
    private final CandidateInterviewQueryService candidateInterviewQueryService;
    private final CandidateInterviewAvailabilityService candidateInterviewAvailabilityService;
    private final CandidateInterviewDecisionService candidateInterviewDecisionService;

    @Operation(summary = "查询本人可面试时间")
    @MaxRiskLevel(RiskLevel.HIGH_RISK)
    @GetMapping("/interview-availability")
    public Result<CandidateInterviewAvailabilityVO> getInterviewAvailability() {
        // 查询当前登录候选人的完整可面试时间配置。
        return Result.success(candidateInterviewAvailabilityService.getMyAvailability());
    }

    @Operation(summary = "完整更新本人可面试时间")
    @MaxRiskLevel(RiskLevel.MID_RISK)
    @PutMapping("/interview-availability")
    public Result<CandidateInterviewAvailabilityVO> updateInterviewAvailability(
            @Valid @RequestBody CandidateInterviewAvailabilityUpdateReq req) {
        // 按客户端读取到的版本号完整替换可面试时间配置。
        return Result.success(candidateInterviewAvailabilityService.updateMyAvailability(req));
    }

    @Operation(summary = "C端查询我的面试排期（分页）")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @GetMapping("/interview-schedules")
    public Result<IPage<InterviewScheduleCandidateListItemVO>> listMySchedules(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) InterviewScheduleStatus status,
            @RequestParam(defaultValue = "interviewTime") String sort,
            @RequestParam(defaultValue = "asc") String order) {
        return Result.success(candidateInterviewQueryService.pageMySchedules(
                page, size, status, sort, order));
    }

    @Operation(summary = "C端确认面试邀请")
    @PostMapping("/interview-schedules/{scheduleId}/confirm")
    public Result<InterviewDecisionVO> confirmSchedule(
            @PathVariable Long scheduleId,
            @Valid @RequestBody InterviewDecisionReq req) {
        return Result.success(candidateInterviewDecisionService.confirmSchedule(scheduleId, req));
    }

    @Operation(summary = "C端拒绝面试邀请")
    @PostMapping("/interview-schedules/{scheduleId}/decline")
    public Result<InterviewDecisionVO> declineSchedule(
            @PathVariable Long scheduleId,
            @Valid @RequestBody InterviewDecisionReq req) {
        return Result.success(candidateInterviewDecisionService.declineSchedule(scheduleId, req));
    }

    @Operation(summary = "C端查询我的面评报告列表（分页）")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @GetMapping("/reports")
    public Result<IPage<InterviewReportCandidateListItemVO>> listMyReports(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false)
            InterviewReportGenerationStatus generationStatus) {
        Long userId = AuthContext.getRequiredUserId();
        return Result.success(interviewReportService.pageCandidateReports(userId, page, size, generationStatus));
    }

    @Operation(summary = "C端查询本人排期报告")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @GetMapping("/reports/{scheduleId}")
    public Result<InterviewReportVO> getMyReport(@PathVariable Long scheduleId) {
        Long userId = AuthContext.getRequiredUserId();
        return Result.success(interviewReportService.getCandidateReport(userId, scheduleId));
    }

    @Operation(summary = "获取本人报告PDF短期下载地址")
    @GetMapping("/reports/{scheduleId}/download")
    public Result<InterviewReportDownloadVO> downloadReport(@PathVariable Long scheduleId) {
        Long userId = AuthContext.getRequiredUserId();
        return Result.success(interviewReportService.getDownloadUrl(userId, scheduleId));
    }
}
