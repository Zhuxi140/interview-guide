package interview.ai.matching.api;

import interview.ai.llm.model.AiResult;
import interview.ai.llm.model.LlmConfigSnapshotMapper;
import interview.ai.matching.model.CandidateJobMatchModelOutput;
import interview.ai.matching.model.HrScreeningModelOutput;
import interview.ai.matching.service.JobMatchingAiService;
import interview.api.aicore.JobMatchingAiApi;
import interview.api.aicore.dto.*;
import interview.api.system.EnterpriseValidationApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 岗位匹配 AI 内部接口实现。
 */
@Service
@RequiredArgsConstructor
public class JobMatchingAiApiImpl implements JobMatchingAiApi {

    private final JobMatchingAiService jobMatchingAiService;

    @Override
    public AiHrScreeningResult screenForHr(
            AiCandidateProfileInput profile, AiJobRequirementInput job) {
        // HR 场景只返回匹配依据，阈值判断由 BPortal 确定性执行。
        AiResult<HrScreeningModelOutput> result =
                jobMatchingAiService.screenForHr(profile, job);
        return new AiHrScreeningResult(
                result.data().overallMatchScore(),
                result.data().dimensionMatches(),
                LlmConfigSnapshotMapper.toDto(result.llmConfigSnapshot()));
    }

    @Override
    public AiCandidateJobMatchResult matchForCandidate(
            AiCandidateProfileInput profile, AiJobRequirementInput job) {
        // 候选人场景独立调用，不接收任何 HR 阈值或审核数据。
        AiResult<CandidateJobMatchModelOutput> result =
                jobMatchingAiService.matchForCandidate(profile, job);
        return new AiCandidateJobMatchResult(
                result.data().matchScore(),
                result.data().passProbability(),
                result.data().strengths(),
                result.data().gaps(),
                LlmConfigSnapshotMapper.toDto(result.llmConfigSnapshot()));
    }


}
