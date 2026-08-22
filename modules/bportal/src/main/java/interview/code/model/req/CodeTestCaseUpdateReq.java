package interview.code.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 部分更新测试用例请求（平台全局题与企业私有题共用）。
 */
@Data
@Schema(description = "部分更新测试用例请求")
public class CodeTestCaseUpdateReq {

    @Size(max = 8000, message = "输入用例不能超过 8000 个字符")
    @Schema(description = "输入测试用例参数原文", example = "[2, 7, 11, 15]\n9")
    private String inputCase;

    @Size(max = 8000, message = "期望输出不能超过 8000 个字符")
    @Schema(description = "期望的标准判定输出结果", example = "[0, 1]")
    private String expectedOutput;

    @Schema(description = "是否为隐藏黑盒边界用例", example = "false")
    private Boolean isSecret;

    @NotNull(message = "期望版本不能为空")
    @Min(value = 0, message = "期望版本不能小于 0")
    @Schema(description = "客户端读取用例时得到的版本号，用于乐观锁", example = "0")
    private Integer expectedVersion;
}
