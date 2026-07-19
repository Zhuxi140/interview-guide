package interview.resume.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 技能评分项响应
 */
@Builder
@Schema(description = "技能评分项响应")
public record CandidateSkillScoreVO(
        @Schema(description = "评分项ID")
        Long id,
        @Schema(description = "维度编码")
        String dimensionCode,
        @Schema(description = "分数")
        Integer score,
        @Schema(description = "AI 理由")
        String aiJustification,
        @Schema(description = "创建时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime createdAt
) {
}
