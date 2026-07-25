package interview.textinterview.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.InterviewReportGenerationStatus;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import interview.textinterview.model.vo.InterviewReportListItemVO;
import interview.textinterview.model.vo.InterviewReportVO;
import interview.textinterview.service.InterviewReportService;
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

/**
 * 企业端面试报告只读接口。
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/enterprises/{enterpriseId}")
@Tag(name = "面试报告（B端）")
@RequiredArgsConstructor
@Validated
public class EnterpriseInterviewReportController {

    private final InterviewReportService interviewReportService;

    @Operation(summary = "查询指定排期报告")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(
            permissions = Perm.InterviewReport.DETAIL,
            scope = PermissionScope.ENTERPRISE
    )
    @GetMapping("/interview-schedules/{scheduleId}/report")
    public Result<InterviewReportVO> getReport(
            @PathVariable Long enterpriseId,
            @PathVariable Long scheduleId) {
        return Result.success(
                interviewReportService.getReportBySchedule(
                        enterpriseId, scheduleId));
    }

    @Operation(summary = "查询企业面试报告列表（分页）")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(
            permissions = Perm.InterviewReport.LIST,
            scope = PermissionScope.ENTERPRISE
    )
    @GetMapping("/interview-reports")
    public Result<IPage<InterviewReportListItemVO>> listReports(
            @PathVariable Long enterpriseId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false)
            InterviewReportGenerationStatus generationStatus,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(interviewReportService.pageReports(
                enterpriseId,
                page,
                size,
                generationStatus,
                startTime,
                endTime
        ));
    }
}
