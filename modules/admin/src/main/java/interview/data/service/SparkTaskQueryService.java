package interview.data.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.data.model.vo.SparkTaskDetailVO;
import interview.data.model.vo.SparkTaskListItemVO;

/**
 * Spark 离线语料任务查询服务。
 *
 * @author zhuxi
 */
public interface SparkTaskQueryService {

    /**
     * 分页查询离线语料任务。
     *
     * @param page   页码
     * @param size   每页条数
     * @param status 任务状态筛选，可为空
     * @return 任务分页结果
     */
    IPage<SparkTaskListItemVO> pageTasks(Integer page, Integer size, String status);

    /**
     * 查询语料任务详情（含入库统计）。
     *
     * @param taskId 任务 ID
     * @return 任务详情
     */
    SparkTaskDetailVO getTaskDetail(Long taskId);
}
