package interview.interviewcfg.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.interviewcfg.model.entity.WorkflowTransitionLog;
import interview.interviewcfg.model.vo.WorkflowLogListItemVO;

public interface WorkflowTransitionLogService extends IService<WorkflowTransitionLog> {

    /**
     * 查询人才流转状态机历史日志（分页）
     * @param enterpriseId 企业ID
     * @param page 页码
     * @param size 每页条数
     * @param scheduleId 排期ID
     * @return 日志分页
     */
    IPage<WorkflowLogListItemVO> pageLogs(Long enterpriseId, Integer page, Integer size, Long scheduleId);
}
