package interview.resume.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

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
        OffsetDateTime analyzedAt
) {
}
