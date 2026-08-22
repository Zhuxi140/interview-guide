package interview.data.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.data.mapper.SparkCorpusTaskMapper;
import interview.data.model.entity.SparkCorpusTask;
import interview.data.model.enums.SparkTaskStatus;
import interview.data.model.vo.SparkTaskDetailVO;
import interview.data.model.vo.SparkTaskListItemVO;
import interview.data.service.SparkTaskQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spark 离线语料任务查询服务实现。
 *
 * @author zhuxi
 */
@Service
@RequiredArgsConstructor
public class SparkTaskQueryServiceImpl
        extends ServiceImpl<SparkCorpusTaskMapper, SparkCorpusTask>
        implements SparkTaskQueryService {

    @Override
    public IPage<SparkTaskListItemVO> pageTasks(Integer page, Integer size, String status) {
        // 校验分页与状态枚举参数。
        validatePage(page, size);
        String statusFilter = normalizeStatus(status);

        // 单表 LambdaQuery：按启动时间倒序稳定分页。
        IPage<SparkCorpusTask> taskPage = lambdaQuery()
                .select(SparkCorpusTask::getId, SparkCorpusTask::getTaskNo,
                        SparkCorpusTask::getStatus, SparkCorpusTask::getRawCount,
                        SparkCorpusTask::getCleanedCount, SparkCorpusTask::getChunkCount,
                        SparkCorpusTask::getStartedAt)
                .eq(statusFilter != null, SparkCorpusTask::getStatus, statusFilter)
                .orderByDesc(SparkCorpusTask::getStartedAt)
                .orderByDesc(SparkCorpusTask::getId)
                .page(new Page<>(page, size));

        // 组装列表 VO。
        List<SparkTaskListItemVO> records = taskPage.getRecords().stream()
                .map(task -> new SparkTaskListItemVO(
                        task.getId(),
                        task.getTaskNo(),
                        task.getStatus(),
                        task.getRawCount(),
                        task.getCleanedCount(),
                        task.getChunkCount(),
                        task.getStartedAt()))
                .toList();
        Page<SparkTaskListItemVO> voPage =
                new Page<>(taskPage.getCurrent(), taskPage.getSize(), taskPage.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public SparkTaskDetailVO getTaskDetail(Long taskId) {
        // 查询任务并校验存在。
        SparkCorpusTask task = getById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.SPARK_TASK_NOT_FOUND);
        }
        // TODO: Spark 执行器接入后，running 状态任务可在此追加实时进度统计。
        return new SparkTaskDetailVO(
                task.getId(),
                task.getTaskNo(),
                task.getSourcePath(),
                task.getStatus(),
                task.getRawCount(),
                task.getCleanedCount(),
                task.getChunkCount(),
                task.getVectorDbWriteLatencyMs(),
                task.getEmbeddingApiLatencyMs(),
                task.getStartedAt());
    }

    private void validatePage(Integer page, Integer size) {
        if (page == null || page < 1 || size == null || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.PAGE_PARAM_INVALID);
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
            return SparkTaskStatus.valueOf(status).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "不支持的任务状态");
        }
    }
}
