package interview.api.aicore.dto;

import java.util.List;

/**
 * 岗位匹配使用的人才画像输入。
 */
public record AiCandidateProfileInput(
        String summary,
        List<AiCandidateDimensionScore> dimensions
) {
}
