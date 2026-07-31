package interview.matching.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import interview.api.aicore.dto.AiCandidateDimensionScore;
import interview.api.aicore.dto.AiCandidateProfileInput;
import interview.api.aicore.dto.AiJobRequirementInput;
import interview.common.enums.AiTaskStatus;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.job.model.entity.Job;
import interview.resume.mapper.CandidateProfileMapper;
import interview.resume.mapper.CandidateSkillScoresMapper;
import interview.resume.model.entity.CandidateProfile;
import interview.resume.model.entity.CandidateSkillScores;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 从已落库画像和岗位快照构建 AICore 输入。
 */
@Service
@RequiredArgsConstructor
public class MatchingAiInputService {

    private final CandidateProfileMapper candidateProfileMapper;
    private final CandidateSkillScoresMapper candidateSkillScoresMapper;
    private final ObjectMapper objectMapper;

    /**
     * 构建岗位无关人才画像输入。
     *
     * @param profileId 画像 ID
     * @return AICore 人才画像输入
     */
    public AiCandidateProfileInput loadProfile(Long profileId) {
        CandidateProfile profile = candidateProfileMapper.selectOne(
                Wrappers.<CandidateProfile>lambdaQuery()
                        .select(CandidateProfile::getSummaryJson,
                                CandidateProfile::getStatus)
                        .eq(CandidateProfile::getId, profileId));
        if (profile == null || profile.getStatus() != AiTaskStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.CANDIDATE_PROFILE_STATUS_INVALID);
        }
        List<CandidateSkillScores> scores = candidateSkillScoresMapper.selectList(
                Wrappers.<CandidateSkillScores>lambdaQuery()
                        .eq(CandidateSkillScores::getCandidateProfileId, profileId)
                        .orderByAsc(CandidateSkillScores::getDimensionCode));
        List<AiCandidateDimensionScore> dimensions = scores.stream()
                .map(score -> new AiCandidateDimensionScore(
                        score.getDimensionCode(), score.getScore(),
                        score.getAiJustification(), readEvidence(score.getEvidenceJson())))
                .toList();
        return new AiCandidateProfileInput(
                readSummary(profile.getSummaryJson()), dimensions);
    }

    /**
     * 从岗位实体生成可持久化的公开要求快照。
     *
     * @param job 岗位实体
     * @return 岗位要求快照
     */
    public AiJobRequirementInput toJobSnapshot(Job job) {
        return new AiJobRequirementInput(
                job.getTitle(), job.getJdContent(), job.getExperienceReq(),
                job.getEducationReq(), readSkills(job.getSkillsJson()));
    }

    /**
     * 读取任务创建时保存的岗位要求快照。
     *
     * @param json 岗位快照 JSON
     * @return 岗位要求
     */
    public AiJobRequirementInput readJobSnapshot(String json) {
        try {
            return objectMapper.readValue(json, AiJobRequirementInput.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }
    }

    private String readSummary(String json) {
        try {
            Map<?, ?> value = objectMapper.readValue(json, Map.class);
            Object summary = value.get("summary");
            return summary == null ? "" : summary.toString();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }
    }

    private List<String> readEvidence(String json) {
        try {
            return Arrays.asList(objectMapper.readValue(json, String[].class));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }
    }

    private List<String> readSkills(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return Arrays.asList(objectMapper.readValue(json, String[].class));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR);
        }
    }
}
