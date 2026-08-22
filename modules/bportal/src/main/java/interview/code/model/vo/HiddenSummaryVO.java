package interview.code.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 隐藏用例汇总响应（只返回计数，不暴露输入与预期输出）。
 */
@Builder
@Schema(description = "隐藏用例汇总响应")
public record HiddenSummaryVO(
        @Schema(description = "隐藏用例通过数", example = "3")
        Long passedCount,
        @Schema(description = "隐藏用例总数", example = "5")
        Long totalCount
) {
}
