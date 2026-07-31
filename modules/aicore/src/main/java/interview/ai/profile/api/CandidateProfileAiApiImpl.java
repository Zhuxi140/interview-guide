package interview.ai.profile.api;

import interview.ai.llm.model.AiResult;
import interview.ai.llm.model.LlmConfigSnapshotMapper;
import interview.ai.profile.model.CandidateProfileModelOutput;
import interview.ai.profile.service.CandidateProfileAiService;
import interview.api.aicore.CandidateProfileAiApi;
import interview.api.aicore.dto.AiCandidateProfileResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 人才画像 AI 内部接口实现。
 */
@Service
@RequiredArgsConstructor
public class CandidateProfileAiApiImpl implements CandidateProfileAiApi {

    private final CandidateProfileAiService candidateProfileAiService;

    @Override
    public AiCandidateProfileResult generateProfile(String resumeText) {
        // 模型只生成业务结果，配置快照由服务端附加。
        AiResult<CandidateProfileModelOutput> result =
                candidateProfileAiService.generate(resumeText);
        return new AiCandidateProfileResult(
                result.data().summary(),
                result.data().dimensions(),
                LlmConfigSnapshotMapper.toDto(result.llmConfigSnapshot()));
    }
}
