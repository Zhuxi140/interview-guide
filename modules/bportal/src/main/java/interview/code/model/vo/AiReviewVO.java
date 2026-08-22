package interview.code.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

/**
 * AI 代码审查结果响应。
 */
@Builder
@Schema(description = "AI 代码审查结果响应")
public record AiReviewVO(
        @Schema(description = "重构与优化建议列表")
        List<String> suggestions,
        @Schema(description = "问题列表（复杂度、潜在 bug 等）")
        List<AiReviewIssueVO> issues
) {
}
