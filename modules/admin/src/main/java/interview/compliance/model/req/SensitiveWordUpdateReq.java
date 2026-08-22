package interview.compliance.model.req;

import interview.compliance.model.enums.SensitiveAction;
import interview.compliance.model.enums.SensitiveCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 编辑敏感词请求（半量更新 + 乐观锁）。
 */
@Data
@Schema(description = "编辑敏感词请求")
public class SensitiveWordUpdateReq {

    @Size(max = 64, message = "敏感词不能超过 64 个字符")
    @Schema(description = "敏感词汇本体；不传保持不变", example = "外挂")
    private String word;

    @Schema(description = "类别；不传保持不变", example = "CHEAT")
    private SensitiveCategory category;

    @Schema(description = "触发动作；不传保持不变", example = "BLOCK")
    private SensitiveAction actionType;

    @NotNull(message = "期望版本号不能为空")
    @Min(value = 0, message = "版本号不能小于 0")
    @Max(value = Integer.MAX_VALUE, message = "版本号超出范围")
    @Schema(description = "期望版本号（乐观锁）", example = "0")
    private Integer expectedVersion;
}
