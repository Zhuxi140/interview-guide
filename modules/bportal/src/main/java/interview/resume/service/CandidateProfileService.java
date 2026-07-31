package interview.resume.service;

import interview.resume.model.entity.CandidateProfile;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 候选人画像服务接口
 * </p>
 *
 * @author zhuxi
 * @since 2026-07-13
 */
public interface CandidateProfileService extends IService<CandidateProfile> {

    /**
     * 查询或创建指定简历当前规范版本的人才画像任务。
     *
     * @param resumeId 简历 ID
     * @param candidateId 候选人 ID
     * @param sourceApplicationId 首次触发投递 ID
     * @param sourceEnterpriseId 首次触发企业 ID
     * @param createdBy 发起用户 ID
     * @return 已完成或正在生成的人才画像
     */
    CandidateProfile ensureProfile(Long resumeId,
                                   Long candidateId,
                                   Long sourceApplicationId,
                                   Long sourceEnterpriseId,
                                   Long createdBy);

    /**
     * 查询可复用的已完成人才画像。
     *
     * @param resumeId 简历 ID
     * @param candidateId 候选人 ID
     * @return 已完成人才画像；不存在时返回 null
     */
    CandidateProfile findReusableProfile(Long resumeId, Long candidateId);
}
