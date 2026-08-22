package interview.compliance.model.vo;

import interview.compliance.model.enums.SensitiveAction;
import interview.compliance.model.enums.SensitiveCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 编辑敏感词结果。
 */
@Builder
@Schema(description = "编辑敏感词结果")
public record SensitiveWordUpdateVO(
        @Schema(description = "敏感词 ID")
        Long id,
        @Schema(description = "敏感词汇本体", example = "作弊工具")
        String word,
        @Schema(description = "类别", example = "CHEAT")
        SensitiveCategory category,
        @Schema(description = "触发动作", example = "BLOCK")
        SensitiveAction actionType,
        @Schema(description = "更新后并发版本号", example = "1")
        Integer version
) {
}
