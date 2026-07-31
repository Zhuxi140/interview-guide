package interview.api.aicore.dto;

import interview.common.enums.CandidateDimensionCode;

import java.util.List;

/**
 * 人才画像维度评分。
 */
public record AiCandidateDimensionScore(
        CandidateDimensionCode dimensionCode,
        Integer score,
        String justification,
        List<String> evidence
) {
}
