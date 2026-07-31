package interview.matching.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import interview.common.enums.CandidateDimensionCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 岗位 AI 初筛配置响应。
 */
@Schema(description = "岗位 AI 初筛配置响应")
public record JobScreeningConfigVO(
        @Schema(description = "岗位 ID")
        Long jobId,
        @Schema(description = "企业 ID")
        Long enterpriseId,
        @Schema(description = "是否自动发起 AI 初筛")
        Boolean enabled,
        @Schema(description = "总体匹配分阈值")
        Integer overallThreshold,
        @Schema(description = "各画像维度最低分")
        Map<CandidateDimensionCode, Integer> dimensionThresholds,
        @Schema(description = "配置版本")
        Integer version,
        @Schema(description = "最后更新时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime updatedAt
) {
}
