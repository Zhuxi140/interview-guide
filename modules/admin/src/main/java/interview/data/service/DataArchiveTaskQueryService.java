package interview.data.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.data.model.vo.ArchiveTaskDetailVO;
import interview.data.model.vo.ArchiveTaskListItemVO;

/**
 * 数据归档任务查询服务。
 *
 * @author zhuxi
 */
public interface DataArchiveTaskQueryService {

    /**
     * 分页查询归档任务。
     *
     * @param page        页码
     * @param size        每页条数
     * @param resourceType 资源类型筛选，可为空
     * @param status      任务状态筛选，可为空
     * @param startTime   创建时间下界（ISO-8601 带时区），可为空
     * @param endTime     创建时间上界（ISO-8601 带时区），可为空
     * @return 归档任务分页结果
     */
    IPage<ArchiveTaskListItemVO> pageTasks(Integer page, Integer size, String resourceType,
                                           String status, String startTime, String endTime);

    /**
     * 查询归档任务状态、统计和失败原因。
     *
     * @param taskId 任务 ID
     * @return 归档任务详情
     */
    ArchiveTaskDetailVO getTaskDetail(Long taskId);
}
