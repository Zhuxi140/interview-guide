package interview.matching.model.bo;

import interview.common.enums.CandidateDimensionCode;

import java.util.Map;

/**
 * HR 初筛阈值快照。
 */
public record ScreeningThresholdSnapshot(
        Integer overallThreshold,
        Map<CandidateDimensionCode, Integer> dimensionThresholds
) {
}
