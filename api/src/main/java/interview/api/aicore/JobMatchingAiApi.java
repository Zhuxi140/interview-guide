package interview.api.aicore;

import interview.api.aicore.dto.*;
import interview.common.exception.BusinessException;

/**
 * 岗位匹配 AI 内部接口。
 */
public interface JobMatchingAiApi {

    /**
     * 计算 HR 初筛所需的人岗匹配结果。
     *
     * @param profile 人才画像
     * @param job 岗位要求
     * @return HR 人岗匹配结果
     */
    AiHrScreeningResult screenForHr(
            AiCandidateProfileInput profile, AiJobRequirementInput job);

    /**
     * 计算候选人侧岗位适配预测。
     *
     * @param profile 人才画像
     * @param job 岗位公开要求
     * @return 候选人岗位适配结果
     */
    AiCandidateJobMatchResult matchForCandidate(
            AiCandidateProfileInput profile, AiJobRequirementInput job);



}
