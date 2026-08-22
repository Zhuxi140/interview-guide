package interview.code.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建编程题请求（平台全局题与企业私有题共用）。
 */
@Data
@Schema(description = "创建编程题请求")
public class CodeQuestionCreateReq {

    @NotBlank(message = "题目标题不能为空")
    @Size(max = 128, message = "题目标题不能超过 128 个字符")
    @Schema(description = "题目名称", example = "两数之和")
    private String title;

    @NotBlank(message = "题目描述不能为空")
    @Size(max = 20000, message = "题目描述不能超过 20000 个字符")
    @Schema(description = "题目详细描述（Markdown）")
    private String description;

    @NotNull(message = "时间限制不能为空")
    @Min(value = 100, message = "时间限制不能低于 100ms")
    @Max(value = 60000, message = "时间限制不能超过 60000ms")
    @Schema(description = "沙箱运行时间限制（毫秒）", example = "1000")
    private Integer timeLimitMs;

    @NotNull(message = "内存限制不能为空")
    @Min(value = 16, message = "内存限制不能低于 16MB")
    @Max(value = 1024, message = "内存限制不能超过 1024MB")
    @Schema(description = "沙箱运行内存限制（MB）", example = "256")
    private Integer memoryLimitMb;

    @NotEmpty(message = "至少支持一种编程语言")
    @Size(max = 8, message = "支持的编程语言不能超过 8 种")
    @Schema(description = "允许提交的编程语言列表",
            example = "[\"JAVA\", \"PYTHON\"]")
    private List<@NotBlank(message = "编程语言不能为空")
                @Size(max = 32, message = "单个编程语言不能超过 32 个字符") String> supportedLanguages;
}
