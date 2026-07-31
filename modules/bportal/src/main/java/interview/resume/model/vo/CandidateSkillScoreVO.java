package interview.resume.model.vo;

import interview.common.enums.CandidateDimensionCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 人才画像固定维度评分响应。
 */
@Schema(description = "人才画像固定维度评分响应")
public record CandidateSkillScoreVO(
        @Schema(description = "维度编码")
        CandidateDimensionCode dimensionCode,
        @Schema(description = "维度分数")
        Integer score,
        @Schema(description = "评分依据")
        String aiJustification,
        @Schema(description = "来自简历的证据")
        List<String> evidence
) {
}
