package interview.data.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 操作审计字段级变更（业务字段名 + 脱敏前后值）。
 */
@Schema(description = "操作审计字段级变更")
public record OperateLogChangeVO(
        @Schema(description = "业务字段名", example = "title")
        String field,
        @Schema(description = "修改前的脱敏值")
        Object before,
        @Schema(description = "修改后的脱敏值")
        Object after
) {
}
