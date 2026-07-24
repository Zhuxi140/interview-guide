package interview.interviewcfg.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.constant.ApiVersion;
import interview.common.constant.Result;
import interview.interviewcfg.model.req.*;
import interview.interviewcfg.model.vo.*;
import interview.interviewcfg.service.InterviewScheduleService;
import interview.interviewcfg.service.WorkflowTransitionLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiVersion.V1 + "/enterprises/{enterpriseId}")
@Tag(name = "面试排期与流转（B端）")
@RequiredArgsConstructor
@Validated
public class InterviewScheduleController {

    private final InterviewScheduleService interviewScheduleService;
    private final WorkflowTransitionLogService workflowTransitionLogService;

    @Operation(summary = "HR 创建排期")
    @PostMapping("/interview-schedules")
    public Result<InterviewScheduleCreateVO> createSchedule(
            @PathVariable Long enterpriseId,
            @Valid @RequestBody InterviewScheduleCreateReq req) {
        return Result.success(interviewScheduleService.createSchedule(enterpriseId, req));
    }

    @Operation(summary = "AI 推荐排期")
    @PostMapping("/interview-schedules/ai-suggestions")
    public Result<List<AiSuggestionItemVO>> suggestSchedule(
            @PathVariable Long enterpriseId,
            @Valid @RequestBody AiSuggestionReq req) {
        return Result.success(interviewScheduleService.suggestSchedule(enterpriseId, req));
    }

    @Operation(summary = "查询企业面试排期列表（分页）")
    @GetMapping("/interview-schedules")
    public Result<IPage<InterviewScheduleListItemVO>> listSchedules(
            @PathVariable Long enterpriseId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "interviewTime") String sort,
            @RequestParam(defaultValue = "asc")
            @Pattern(regexp = "^(?i)(asc|desc)$") String order) {
        return Result.success(interviewScheduleService.pageSchedules(
                enterpriseId, page, size, status, startTime, endTime, sort, order));
    }

    @Operation(summary = "查询排期详情")
    @GetMapping("/interview-schedules/{scheduleId}")
    public Result<InterviewScheduleDetailVO> getScheduleDetail(
            @PathVariable Long enterpriseId,
            @PathVariable Long scheduleId) {
        return Result.success(interviewScheduleService.getScheduleDetail(enterpriseId, scheduleId));
    }

    @Operation(summary = "HR 重新安排面试")
    @PatchMapping("/interview-schedules/{scheduleId}")
    public Result<InterviewScheduleUpdateVO> reschedule(
            @PathVariable Long enterpriseId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody InterviewScheduleRescheduleReq req) {
        return Result.success(interviewScheduleService.reschedule(enterpriseId, scheduleId, req));
    }

    @Operation(summary = "HR 取消排期")
    @PostMapping("/interview-schedules/{scheduleId}/cancel")
    public Result<InterviewScheduleUpdateVO> cancelSchedule(
            @PathVariable Long enterpriseId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody InterviewScheduleCancelReq req) {
        return Result.success(interviewScheduleService.cancelSchedule(enterpriseId, scheduleId, req));
    }

    @Operation(summary = "HR 流转面试最终状态")
    @PatchMapping("/interview-schedules/{scheduleId}/status")
    public Result<InterviewScheduleUpdateVO> updateStatus(
            @PathVariable Long enterpriseId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody InterviewScheduleStatusReq req) {
        return Result.success(interviewScheduleService.updateStatus(enterpriseId, scheduleId, req));
    }

    @Operation(summary = "查询人才流转状态机历史日志（分页）")
    @GetMapping("/workflow-logs")
    public Result<IPage<WorkflowLogListItemVO>> listWorkflowLogs(
            @PathVariable Long enterpriseId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) Long scheduleId) {
        return Result.success(
                workflowTransitionLogService.pageLogs(enterpriseId, page, size, scheduleId));
    }
}
