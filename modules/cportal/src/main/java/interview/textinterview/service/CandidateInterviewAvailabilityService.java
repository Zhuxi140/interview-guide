package interview.textinterview.service;

import com.baomidou.mybatisplus.extension.service.IService;
import interview.textinterview.model.entity.CandidateInterviewAvailability;
import interview.textinterview.model.req.CandidateInterviewAvailabilityUpdateReq;
import interview.textinterview.model.vo.CandidateInterviewAvailabilityVO;

/**
 * 候选人可面试时间服务。
 */
public interface CandidateInterviewAvailabilityService
        extends IService<CandidateInterviewAvailability> {

    /**
     * 查询当前候选人的可面试时间
     * @return 可面试时间配置
     */
    CandidateInterviewAvailabilityVO getMyAvailability();

    /**
     * 完整更新当前候选人的可面试时间
     * @param req 更新请求
     * @return 更新后的可面试时间配置
     */
    CandidateInterviewAvailabilityVO updateMyAvailability(
            CandidateInterviewAvailabilityUpdateReq req);
}
