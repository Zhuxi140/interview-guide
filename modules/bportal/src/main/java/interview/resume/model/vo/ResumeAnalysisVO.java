package interview.resume.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 简历 AI 分析结果响应
 */
@Builder
public record ResumeAnalysisVO(
        Long id,
        Long resumeId,
        Integer overallScore,
        String strengthsJson,
        String suggestionsJson,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime analyzedAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime createdAt
) {
}
