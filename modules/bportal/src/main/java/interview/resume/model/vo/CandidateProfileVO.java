package interview.resume.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 候选人画像（雷达图单项维度）响应
 */
@Builder
@Schema(description = "候选人画像（雷达图单项维度）响应")
public record CandidateProfileVO(
        @Schema(description = "用户ID")
        Long userId,
        @Schema(description = "维度编码")
        String dimensionCode,
        @Schema(description = "平均分")
        Integer avgScore,
        @Schema(description = "最新理由说明")
        String latestJustification,
        @Schema(description = "创建时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime createdAt,
        @Schema(description = "更新时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime updatedAt
) {
}
