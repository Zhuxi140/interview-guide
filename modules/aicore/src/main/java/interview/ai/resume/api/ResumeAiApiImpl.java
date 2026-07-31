package interview.ai.resume.api;

import interview.ai.llm.model.AiResult;
import interview.ai.llm.model.LlmConfigSnapshotMapper;
import interview.ai.resume.service.ResumeAnalysisAiService;
import interview.api.aicore.ResumeAiApi;
import interview.api.aicore.dto.AiResumeAnalysisResult;
import interview.api.aicore.dto.LlmConfigSnapshotDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResumeAiApiImpl implements ResumeAiApi {

    private final ResumeAnalysisAiService resumeAnalysisAiService;

    @Override
    public AiResumeAnalysisResult analysisResume(String resumeText) {
        AiResult<AiResumeAnalysisResult> aiResumeAnalysisResultAiResult = resumeAnalysisAiService.analysisResume(resumeText);

        LlmConfigSnapshotDTO llmConfigSnapshotDTO = LlmConfigSnapshotMapper.toDto(
                aiResumeAnalysisResultAiResult.llmConfigSnapshot());
        return new AiResumeAnalysisResult(
                aiResumeAnalysisResultAiResult.data().overallScore(),
                aiResumeAnalysisResultAiResult.data().strengthsJson(),
                aiResumeAnalysisResultAiResult.data().suggestionsJson(),
                llmConfigSnapshotDTO
        );

    }
}
