package interview.candidate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import interview.candidate.model.entity.CandidateBasicProfile;
import interview.candidate.model.req.CandidateProfileUpdateReq;
import interview.candidate.model.vo.CandidateBasicProfileVO;
import interview.candidate.model.vo.CandidateProfileUpdateVO;

public interface CandidateBasicProfileService extends IService<CandidateBasicProfile> {

    /**
     * 查询当前用户的候选人资料和最新能力维度
     * @return 候选人资料
     */
    CandidateBasicProfileVO getMyProfile();

    /**
     * 按版本部分更新当前用户候选人资料
     * @param req 更新请求
     * @return 更新结果
     */
    CandidateProfileUpdateVO updateMyProfile(CandidateProfileUpdateReq req);
}
