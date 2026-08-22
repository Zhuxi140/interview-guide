package interview.compliance.model.vo;

import interview.compliance.model.enums.SensitiveAction;
import interview.compliance.model.enums.SensitiveCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * 敏感词列表项。
 */
@Builder
@Schema(description = "敏感词列表项")
public record SensitiveWordListItemVO(
        @Schema(description = "敏感词 ID")
        Long id,
        @Schema(description = "敏感词汇本体", example = "外挂")
        String word,
        @Schema(description = "类别", example = "CHEAT")
        SensitiveCategory category,
        @Schema(description = "触发动作", example = "BLOCK")
        SensitiveAction actionType,
        @Schema(description = "并发版本号", example = "0")
        Integer version,
        @Schema(description = "创建时间")
        OffsetDateTime createdAt
) {
}
