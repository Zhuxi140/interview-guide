package interview.api.aicore.dto;

import interview.common.enums.CandidateDimensionCode;

/**
 * 人岗匹配维度结果。
 */
public record AiDimensionMatch(
        CandidateDimensionCode dimensionCode,
        Integer score,
        String explanation
) {
}
