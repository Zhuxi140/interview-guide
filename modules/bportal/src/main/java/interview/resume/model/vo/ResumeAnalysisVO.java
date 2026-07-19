package interview.resume.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author zhuxi
 * @apiNote 简历 AI 分析结果响应
 */
@Builder
@Schema(description = "简历 AI 分析结果响应")
public record ResumeAnalysisVO(
        @Schema(description = "综合评分")
        Integer overallScore,
        @Schema(description = "优势 JSON")
        String strengthsJson,
        @Schema(description = "建议 JSON")
        String suggestionsJson,
        @Schema(description = "分析时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime analyzedAt,
        @Schema(description = "技能评分列表") List<SkillScoreItem> skillScores,
        @Schema(description = "候选人画像列表") List<CandidateProfileItem> candidateProfile
) {

    @Builder
    @Schema(description = "技能评分项")
    public record SkillScoreItem(
            @Schema(description = "维度编码")
            String dimensionCode,
            @Schema(description = "分数")
            Integer score,
            @Schema(description = "AI 理由")
            String aiJustification
    ) {}

    @Builder
    @Schema(description = "候选人画像项")
    public record CandidateProfileItem(
            @Schema(description = "维度编码")
            String dimensionCode,
            @Schema(description = "平均分")
            Integer avgScore,
            @Schema(description = "最新理由说明")
            String latestJustification
    ) {}
}
