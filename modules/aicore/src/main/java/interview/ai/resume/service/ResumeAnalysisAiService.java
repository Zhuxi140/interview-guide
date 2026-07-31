package interview.ai.resume.service;


import interview.ai.llm.client.UnifiedChatClient;
import interview.ai.llm.model.AiResult;
import interview.api.aicore.dto.AiResumeAnalysisResult;
import interview.common.enums.AiSceneCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResumeAnalysisAiService {

    private final UnifiedChatClient unifiedChatClient;

    public AiResult<AiResumeAnalysisResult> analysisResume(String resumeText) {
        return unifiedChatClient.callStructured(
                AiSceneCode.RESUME_ANALYSIS,
                resumeText,
                AiResumeAnalysisResult.class
        );
    }
}
