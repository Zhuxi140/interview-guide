package interview.ai.matching.service;

import interview.ai.llm.client.UnifiedChatClient;
import interview.ai.llm.model.AiResult;
import interview.ai.matching.model.CandidateJobMatchModelOutput;
import interview.ai.matching.model.HrScreeningModelOutput;
import interview.api.aicore.dto.AiCandidateProfileInput;
import interview.api.aicore.dto.AiJobRequirementInput;
import interview.common.enums.AiSceneCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * HR 与候选人岗位匹配模型调用服务。
 */
@Service
@RequiredArgsConstructor
public class JobMatchingAiService {

    private final UnifiedChatClient unifiedChatClient;
    private final ObjectMapper objectMapper;

    /**
     * 调用 HR 人岗匹配场景。
     *
     * @param profile 人才画像
     * @param job 岗位要求
     * @return 模型结果及配置快照
     */
    public AiResult<HrScreeningModelOutput> screenForHr(
            AiCandidateProfileInput profile, AiJobRequirementInput job) {
        return unifiedChatClient.callStructured(
                AiSceneCode.HR_APPLICATION_SCREENING,
                buildPrompt(profile, job),
                HrScreeningModelOutput.class);
    }

    /**
     * 调用候选人岗位预测场景。
     *
     * @param profile 人才画像
     * @param job 岗位公开要求
     * @return 模型结果及配置快照
     */
    public AiResult<CandidateJobMatchModelOutput> matchForCandidate(
            AiCandidateProfileInput profile, AiJobRequirementInput job) {
        return unifiedChatClient.callStructured(
                AiSceneCode.CANDIDATE_JOB_MATCHING,
                buildPrompt(profile, job),
                CandidateJobMatchModelOutput.class);
    }

    private String buildPrompt(
            AiCandidateProfileInput profile, AiJobRequirementInput job) {
        return objectMapper.writeValueAsString(Map.of(
                "candidateProfile", profile,
                "jobRequirement", job));
    }
}
