package interview.matching.service;

import com.baomidou.mybatisplus.extension.service.IService;
import interview.matching.model.entity.CandidateJobMatchAnalysis;
import interview.matching.model.vo.CandidateJobMatchAnalysisVO;
import interview.matching.model.vo.CandidateJobMatchTriggerVO;

/**
 * 候选人岗位适配预测服务接口。
 */
public interface CandidateJobMatchAnalysisService
        extends IService<CandidateJobMatchAnalysis> {

    /**
     * 候选人对自己的投递发起岗位适配预测。
     *
     * @param applicationId 投递 ID
     * @param idempotencyKey 幂等键
     * @return 任务受理结果
     */
    CandidateJobMatchTriggerVO accept(
            Long applicationId, String idempotencyKey);

    /**
     * 查询当前候选人一条投递的最新岗位适配预测。
     *
     * @param applicationId 投递 ID
     * @return 最新预测结果
     */
    CandidateJobMatchAnalysisVO getLatest(Long applicationId);
}
