package interview.code.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建测试用例请求（平台全局题与企业私有题共用）。
 */
@Data
@Schema(description = "创建测试用例请求")
public class CodeTestCaseCreateReq {

    @NotBlank(message = "输入用例不能为空")
    @Size(max = 8000, message = "输入用例不能超过 8000 个字符")
    @Schema(description = "输入测试用例参数原文", example = "[2, 7, 11, 15]\n9")
    private String inputCase;

    @NotBlank(message = "期望输出不能为空")
    @Size(max = 8000, message = "期望输出不能超过 8000 个字符")
    @Schema(description = "期望的标准判定输出结果", example = "[0, 1]")
    private String expectedOutput;

    @Schema(description = "是否为隐藏黑盒边界用例，默认 false", example = "false")
    private Boolean isSecret = false;
}
