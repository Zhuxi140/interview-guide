package interview.code.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * AI 代码审查单条问题响应。
 */
@Builder
@Schema(description = "AI 代码审查问题响应")
public record AiReviewIssueVO(
        @Schema(description = "问题严重级别，如 INFO / WARNING / ERROR", example = "WARNING")
        String severity,
        @Schema(description = "问题说明")
        String description,
        @Schema(description = "关联代码行号（1 开始）；无法定位时为 null", example = "12")
        Integer line
) {
}
