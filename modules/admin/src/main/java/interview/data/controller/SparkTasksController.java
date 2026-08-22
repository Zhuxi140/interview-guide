package interview.data.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import interview.data.model.vo.SparkTaskDetailVO;
import interview.data.model.vo.SparkTaskListItemVO;
import interview.data.service.SparkTaskQueryService;
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
 * Spark 离线语料任务查询（Admin）。
 *
 * @author zhuxi
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/admin/spark-tasks")
@Tag(name = "离线语料任务（Admin）")
@RequiredArgsConstructor
@Validated
public class SparkTasksController {

    private final SparkTaskQueryService sparkTaskQueryService;

    @Operation(summary = "查询离线语料同步任务列表（分页）")
    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(
            permissions = Perm.SparkTasks.LIST,
            scope = PermissionScope.PLATFORM
    )
    @GetMapping
    public Result<IPage<SparkTaskListItemVO>> listTasks(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) String status) {
        return Result.success(sparkTaskQueryService.pageTasks(page, size, status));
    }

    @Operation(summary = "查询语料任务详情（含入库统计）")
    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @RequirePermission(
            permissions = Perm.SparkTasks.DETAIL,
            scope = PermissionScope.PLATFORM
    )
    @GetMapping("/{taskId}")
    public Result<SparkTaskDetailVO> getTaskDetail(@PathVariable Long taskId) {
        return Result.success(sparkTaskQueryService.getTaskDetail(taskId));
    }
}
