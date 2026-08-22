package interview.compliance.model.req;

import interview.compliance.model.enums.SensitiveAction;
import interview.compliance.model.enums.SensitiveCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 添加敏感词请求。
 */
@Data
@Schema(description = "添加敏感词请求")
public class SensitiveWordCreateReq {

    @NotBlank(message = "敏感词不能为空")
    @Size(max = 64, message = "敏感词不能超过 64 个字符")
    @Schema(description = "敏感词汇本体", example = "外挂")
    private String word;

    @NotNull(message = "类别不能为空")
    @Schema(description = "类别：POLITICAL / PROFANITY / CHEAT", example = "CHEAT")
    private SensitiveCategory category;

    @Schema(description = "触发动作：BLOCK / REPLACE / ALERT；缺省为 BLOCK", example = "BLOCK")
    private SensitiveAction actionType;
}
