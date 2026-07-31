package interview.ai.profile.service;

import interview.ai.llm.client.UnifiedChatClient;
import interview.ai.llm.model.AiResult;
import interview.ai.profile.model.CandidateProfileModelOutput;
import interview.common.enums.AiSceneCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 人才画像模型调用服务。
 */
@Service
@RequiredArgsConstructor
public class CandidateProfileAiService {

    private final UnifiedChatClient unifiedChatClient;

    /**
     * 调用岗位无关的人才画像场景。
     *
     * @param resumeText 简历正文
     * @return 模型结果及配置快照
     */
    public AiResult<CandidateProfileModelOutput> generate(String resumeText) {
        return unifiedChatClient.callStructured(
                AiSceneCode.CANDIDATE_PROFILE_GENERATION,
                "请根据以下简历正文生成人才画像：\n\n" + resumeText,
                CandidateProfileModelOutput.class);
    }
}
