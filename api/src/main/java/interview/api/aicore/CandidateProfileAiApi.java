package interview.api.aicore;

import interview.api.aicore.dto.AiCandidateProfileResult;

/**
 * 人才画像 AI 内部接口。
 */
public interface CandidateProfileAiApi {

    /**
     * 根据简历正文生成岗位无关的人才画像。
     *
     * @param resumeText 简历正文
     * @return 人才画像结果
     */
    AiCandidateProfileResult generateProfile(String resumeText);
}
