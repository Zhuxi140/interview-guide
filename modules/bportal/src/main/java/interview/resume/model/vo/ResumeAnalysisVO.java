package interview.resume.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author zhuxi
 * @apiNote 简历 AI 分析结果响应
 */
@Builder
public record ResumeAnalysisVO(
        Integer overallScore,
        String strengthsJson,
        String suggestionsJson,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime analyzedAt,
        List<SkillScoreItem> skillScores,
        List<CandidateProfileItem> candidateProfile
) {

    @Builder
    public record SkillScoreItem(
            String dimensionCode,
            Integer score,
            String aiJustification
    ) {}

    @Builder
    public record CandidateProfileItem(
            String dimensionCode,
            Integer avgScore,
            String latestJustification
    ) {}
}
