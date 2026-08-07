package interview.ai.planDraft.api;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import interview.ai.llm.client.UnifiedChatClient;
import interview.ai.llm.model.AiResult;
import interview.ai.llm.model.LlmConfigSnapshotMapper;
import interview.ai.planDraft.tool.InterviewPlanAgentTools;
import interview.api.aicore.InterviewPlanTradeAiApi;
import interview.api.aicore.dto.AiInterviewPlanInput;
import interview.api.aicore.dto.AiInterviewPlanResult;
import interview.api.system.UserApi;
import interview.common.enums.AiSceneCode;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class InterviewPlanGenerationService implements InterviewPlanTradeAiApi {

    private final UserApi userApi;
    private final UnifiedChatClient unifiedChatClient;


    @Override
    public AiInterviewPlanResult generateInterviewPlan(AiInterviewPlanInput input) {
        InterviewPlanAgentTools tools = new InterviewPlanAgentTools(userApi, input);
        var root = JSONUtil.parseObj(input.requestJson());
        String prompt = root.getStr("prompt", "");
        AiResult<AiInterviewPlanResult> result = unifiedChatClient.callWithAgent(
                AiSceneCode.INTERVIEW_PLAN_GENERATION,
                prompt,
                AiInterviewPlanResult.class,
                ToolCallbacks.from(tools));
        return new AiInterviewPlanResult(
                result.data().stages(),
                result.data().scheduleSuggestions(),
                LlmConfigSnapshotMapper.toDto(result.llmConfigSnapshot()));
    }
}
