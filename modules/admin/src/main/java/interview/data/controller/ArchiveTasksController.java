package interview.data.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import interview.data.model.vo.ArchiveTaskDetailVO;
import interview.data.model.vo.ArchiveTaskListItemVO;
import interview.data.service.DataArchiveTaskQueryService;
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
 * 数据归档任务查询（Admin）。
 *
 * @author zhuxi
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/admin/archive-tasks")
@Tag(name = "数据归档任务（Admin）")
@RequiredArgsConstructor
@Validated
public class ArchiveTasksController {

    private final DataArchiveTaskQueryService dataArchiveTaskQueryService;

    @Operation(summary = "分页查询归档任务")
    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(
            permissions = Perm.Audit.ARCHIVE_TASKS_LIST,
            scope = PermissionScope.PLATFORM
    )
    @GetMapping
    public Result<IPage<ArchiveTaskListItemVO>> listTasks(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(dataArchiveTaskQueryService.pageTasks(
                page, size, resourceType, status, startTime, endTime));
    }

    @Operation(summary = "查询归档任务状态、统计和失败原因")
    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(
            permissions = Perm.Audit.ARCHIVE_TASKS_DETAIL,
            scope = PermissionScope.PLATFORM
    )
    @GetMapping("/{taskId}")
    public Result<ArchiveTaskDetailVO> getTaskDetail(@PathVariable Long taskId) {
        return Result.success(dataArchiveTaskQueryService.getTaskDetail(taskId));
    }
}
