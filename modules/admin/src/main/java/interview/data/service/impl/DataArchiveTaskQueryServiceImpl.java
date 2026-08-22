package interview.data.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.data.mapper.DataArchiveTaskMapper;
import interview.data.model.entity.DataArchiveTask;
import interview.data.model.enums.ArchiveResourceType;
import interview.data.model.enums.ArchiveTaskStatus;
import interview.data.model.vo.ArchiveTaskDetailVO;
import interview.data.model.vo.ArchiveTaskListItemVO;
import interview.data.service.DataArchiveTaskQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 数据归档任务查询服务实现。
 *
 * @author zhuxi
 */
@Service
@RequiredArgsConstructor
public class DataArchiveTaskQueryServiceImpl
        extends ServiceImpl<DataArchiveTaskMapper, DataArchiveTask>
        implements DataArchiveTaskQueryService {

    // TODO: 归档执行器接入后产生任务数据，当前查询接口按空结果正常返回。

    @Override
    public IPage<ArchiveTaskListItemVO> pageTasks(Integer page, Integer size,
                                                  String resourceType, String status,
                                                  String startTime, String endTime) {
        // 校验分页、枚举与时间范围参数。
        validatePage(page, size);
        String type = normalizeResourceType(resourceType);
        String statusFilter = normalizeStatus(status);
        OffsetDateTime start = parseTime(startTime);
        OffsetDateTime end = parseTime(endTime);
        validateTimeRange(start, end);

        // 单表 LambdaQuery：按创建时间倒序稳定分页。
        IPage<DataArchiveTask> taskPage = lambdaQuery()
                .select(DataArchiveTask::getId, DataArchiveTask::getResourceType,
                        DataArchiveTask::getStatus, DataArchiveTask::getArchivedCount,
                        DataArchiveTask::getCreatedAt, DataArchiveTask::getCompletedAt)
                .eq(type != null, DataArchiveTask::getResourceType, type)
                .eq(statusFilter != null, DataArchiveTask::getStatus, statusFilter)
                .ge(start != null, DataArchiveTask::getCreatedAt, start)
                .le(end != null, DataArchiveTask::getCreatedAt, end)
                .orderByDesc(DataArchiveTask::getCreatedAt)
                .orderByDesc(DataArchiveTask::getId)
                .page(new Page<>(page, size));

        // 组装列表 VO。
        List<ArchiveTaskListItemVO> records = taskPage.getRecords().stream()
                .map(task -> new ArchiveTaskListItemVO(
                        task.getId(),
                        task.getResourceType(),
                        task.getStatus(),
                        task.getArchivedCount(),
                        task.getCreatedAt(),
                        task.getCompletedAt()))
                .toList();
        Page<ArchiveTaskListItemVO> voPage =
                new Page<>(taskPage.getCurrent(), taskPage.getSize(), taskPage.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public ArchiveTaskDetailVO getTaskDetail(Long taskId) {
        // 查询任务并校验存在。
        DataArchiveTask task = getById(taskId);
        if (task == null) {
            // TODO: ErrorCode 缺少 ARCHIVE_TASK_NOT_FOUND，暂以参数错误语义返回。
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "归档任务不存在");
        }
        return new ArchiveTaskDetailVO(
                task.getId(),
                task.getResourceType(),
                task.getBeforeTime(),
                task.getStatus(),
                task.getArchivedCount(),
                task.getFailureReason(),
                task.getCreatedAt(),
                task.getCompletedAt());
    }

    private void validatePage(Integer page, Integer size) {
        if (page == null || page < 1 || size == null || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.PAGE_PARAM_INVALID);
        }
    }

    private OffsetDateTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(ErrorCode.TIME_FORMAT_INVALID);
        }
    }

    private void validateTimeRange(OffsetDateTime start, OffsetDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new BusinessException(ErrorCode.TIME_RANGE_INVALID);
        }
    }

    /**
     * 校验并返回合法的资源类型；空白入参返回 null 表示不过滤。
     */
    private String normalizeResourceType(String resourceType) {
        if (resourceType == null || resourceType.isBlank()) {
            return null;
        }
        try {
            return ArchiveResourceType.valueOf(resourceType).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "不支持的资源类型");
        }
    }

    /**
     * 校验并返回合法的任务状态；空白入参返回 null 表示不过滤。
     */
    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ArchiveTaskStatus.valueOf(status).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "不支持的任务状态");
        }
    }
}
