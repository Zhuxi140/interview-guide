package interview.compliance.model.vo;

import interview.compliance.model.enums.SensitiveAction;
import interview.compliance.model.enums.SensitiveCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 添加敏感词结果。
 */
@Builder
@Schema(description = "添加敏感词结果")
public record SensitiveWordCreateVO(
        @Schema(description = "敏感词 ID")
        Long id,
        @Schema(description = "敏感词汇本体", example = "外挂")
        String word,
        @Schema(description = "类别", example = "CHEAT")
        SensitiveCategory category,
        @Schema(description = "触发动作", example = "BLOCK")
        SensitiveAction actionType
) {
}
