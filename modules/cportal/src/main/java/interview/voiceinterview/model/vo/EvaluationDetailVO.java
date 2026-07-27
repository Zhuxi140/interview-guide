package interview.voiceinterview.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author zhuxi
 * @apiNote 语音评估详情
 * @since 2026/7/26 14:05
 */
@Builder
@Schema(description = "语音评估详情")
public record EvaluationDetailVO(
        @Schema(description = "评估记录ID")
        Long id,

        @Schema(description = "总分 0-100")
        Integer overallScore,

        @Schema(description = "逐题评估数组")
        List<Object> questionEvaluations,

        @Schema(description = "亮点列表")
        List<String> strengths,

        @Schema(description = "改进建议列表")
        List<String> improvements,

        @Schema(description = "完成时间")
        OffsetDateTime completedAt
) {
}
